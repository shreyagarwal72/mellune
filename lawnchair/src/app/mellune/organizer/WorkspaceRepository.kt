package app.mellune.organizer

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import app.mellune.predictions.UsageStatsRanker
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import app.mellune.util.categorizeAppsWithSystemAndGoogle
import com.android.launcher3.AppFilter
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.Workspace
import com.android.launcher3.celllayout.CellPosMapper
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelWriter
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.LauncherAppWidgetInfo
import com.android.launcher3.util.IntArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WorkspacePageSummary(
    val screenId: Int,
    val pageIndex: Int,
    val isDefaultPage: Boolean,
    val appCount: Int,
    val folderCount: Int,
    val widgetCount: Int,
    val previewTitles: List<String>,
)

data class CategorizedSpaceData(
    val allApps: List<AppInfo>,
    val categories: Map<String, List<AppInfo>>,
    val mostUsedApps: List<AppInfo>,
)

/**
 * Coordinates workspace page ordering, creation/deletion, and topic-based Space generation
 * using [BgDataModel], [ModelWriter], Flowerpot categorization, and usage frequency signals.
 */
object WorkspaceRepository {

    suspend fun loadWorkspacePages(context: Context): List<WorkspacePageSummary> = withContext(Dispatchers.Main) {
        val model = LauncherAppState.getInstance(context).model
        val defaultPageIndex = PreferenceManager2.getInstance(context).defaultHomePage.firstCached()
        val deferred = CompletableDeferred<List<WorkspacePageSummary>>()
        model.enqueueModelUpdateTask { _, dataModel, _ ->
            val summaries = buildPageSummaries(context, dataModel, defaultPageIndex)
            deferred.complete(summaries)
        }
        deferred.await()
    }

    private fun buildPageSummaries(
        context: Context,
        dataModel: BgDataModel,
        defaultPageIndex: Int,
    ): List<WorkspacePageSummary> {
        val screenOrder: IntArray
        val itemsByScreen = LinkedHashMap<Int, MutableList<ItemInfo>>()
        synchronized(dataModel) {
            screenOrder = dataModel.itemsIdMap.collectWorkspaceScreens(context)
            for (i in 0 until screenOrder.size()) {
                itemsByScreen[screenOrder.get(i)] = mutableListOf()
            }
            dataModel.itemsIdMap.forEach { item ->
                if (item.container == Favorites.CONTAINER_DESKTOP) {
                    itemsByScreen.getOrPut(item.screenId) { mutableListOf() }.add(item)
                }
            }
        }
        val orderedIds = buildList {
            for (i in 0 until screenOrder.size()) add(screenOrder.get(i))
        }
        val clampedDefault = defaultPageIndex.coerceIn(0, (orderedIds.size - 1).coerceAtLeast(0))
        return orderedIds.mapIndexed { index, screenId ->
            val items = itemsByScreen[screenId].orEmpty()
            var apps = 0
            var folders = 0
            var widgets = 0
            items.forEach { item ->
                when (item) {
                    is FolderInfo -> folders++
                    is LauncherAppWidgetInfo -> widgets++
                    else -> apps++
                }
            }
            val previews = items
                .mapNotNull { it.title?.toString()?.takeIf(String::isNotBlank) }
                .distinct()
                .take(4)
            WorkspacePageSummary(
                screenId = screenId,
                pageIndex = index,
                isDefaultPage = index == clampedDefault,
                appCount = apps,
                folderCount = folders,
                widgetCount = widgets,
                previewTitles = previews,
            )
        }
    }

    suspend fun reorderPages(context: Context, orderedScreenIds: List<Int>, defaultScreenId: Int?) {
        if (orderedScreenIds.isEmpty()) return
        val intArray = IntArray().apply {
            orderedScreenIds.forEach { add(it) }
        }
        val model = LauncherAppState.getInstance(context).model
        val writer = model.getWriter(false, CellPosMapper.DEFAULT, null)
        withContext(Dispatchers.IO) {
            writer.persistWorkspaceScreenOrderSync(intArray)
        }
        if (defaultScreenId != null) {
            val newDefaultIdx = orderedScreenIds.indexOf(defaultScreenId)
            if (newDefaultIdx >= 0) {
                PreferenceManager2.getInstance(context).defaultHomePage.set(newDefaultIdx)
            }
        }
        withContext(Dispatchers.Main) {
            model.forceReload()
        }
    }

    suspend fun setDefaultPage(context: Context, pageIndex: Int) {
        PreferenceManager2.getInstance(context).defaultHomePage.set(pageIndex.coerceAtLeast(0))
    }

    suspend fun addBlankPage(context: Context): Int {
        val current = loadWorkspacePages(context)
        val newScreenId = (current.maxOfOrNull { it.screenId } ?: Workspace.FIRST_SCREEN_ID) + 1
        val newOrder = current.map { it.screenId } + newScreenId
        val defaultScreenId = current.firstOrNull { it.isDefaultPage }?.screenId
        reorderPages(context, newOrder, defaultScreenId)
        return newScreenId
    }

    suspend fun deletePage(context: Context, screenId: Int) {
        val model = LauncherAppState.getInstance(context).model
        val prefs2 = PreferenceManager2.getInstance(context)
        val currentPages = loadWorkspacePages(context)
        if (currentPages.size <= 1) return

        val currentDefaultScreenId = currentPages.firstOrNull { it.isDefaultPage }?.screenId
        val remainingIds = currentPages.map { it.screenId }.filter { it != screenId }

        val deferred = CompletableDeferred<Unit>()
        model.enqueueModelUpdateTask { _, dataModel, _ ->
            val writer = model.getWriter(false, CellPosMapper.DEFAULT, null)
            val toDelete = mutableListOf<ItemInfo>()
            synchronized(dataModel) {
                dataModel.itemsIdMap.forEach { item ->
                    if (item.container == Favorites.CONTAINER_DESKTOP && item.screenId == screenId) {
                        toDelete.add(item)
                    }
                }
            }
            if (toDelete.isNotEmpty()) {
                writer.deleteItemsFromDatabase(toDelete, "workspaceOrganizerDeletePage")
            }
            val nextOrder = IntArray().apply {
                remainingIds.forEach { add(it) }
            }
            writer.persistWorkspaceScreenOrderSync(nextOrder)
            deferred.complete(Unit)
        }
        deferred.await()

        val nextDefaultIndex = when {
            currentDefaultScreenId == null || currentDefaultScreenId == screenId -> 0
            else -> remainingIds.indexOf(currentDefaultScreenId).coerceAtLeast(0)
        }
        prefs2.defaultHomePage.set(nextDefaultIndex)
        withContext(Dispatchers.Main) {
            model.forceReload()
        }
    }

    suspend fun loadCategorizedApps(context: Context): CategorizedSpaceData = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val appFilter = AppFilter(context)
        val user = Process.myUserHandle()
        val apps = launcherApps?.getActivityList(null, user)
            .orEmpty()
            .filter { appFilter.shouldShowApp(it.componentName) }
            .map { AppInfo(context, it, user) }
            .sortedBy { it.title?.toString().orEmpty().lowercase() }

        val rankedKeys = runCatching {
            if (launcherApps == null) {
                emptyList()
            } else {
                UsageStatsRanker(context).getUsageStatsRanked(
                    launcherApps = launcherApps,
                    appFilter = appFilter,
                ) { pkg, la, filter ->
                    la.getActivityList(pkg, user)
                        .firstOrNull { filter.shouldShowApp(it.componentName) }
                        ?.componentName
                        ?.packageName
                }
            }
        }.getOrDefault(emptyList())

        val appsByPkg = apps.associateBy { it.targetPackage }
        val mostUsed = rankedKeys.mapNotNull { appsByPkg[it] }.distinct().take(12)
        val categorized = categorizeAppsWithSystemAndGoogle(apps, context)

        CategorizedSpaceData(
            allApps = apps,
            categories = categorized,
            mostUsedApps = mostUsed.ifEmpty { apps.take(8) },
        )
    }

    suspend fun createTopicSpace(
        context: Context,
        spaceName: String,
        apps: List<AppInfo>,
        groupIntoFolder: Boolean,
    ): Int {
        if (apps.isEmpty()) return -1
        val model = LauncherAppState.getInstance(context).model
        val idp = InvariantDeviceProfile.INSTANCE.get(context)
        val cols = idp.numColumns.coerceAtLeast(2)
        val rows = idp.numRows.coerceAtLeast(2)
        val maxSlots = cols * rows

        val existingPages = loadWorkspacePages(context)
        val newScreenId = (existingPages.maxOfOrNull { it.screenId } ?: Workspace.FIRST_SCREEN_ID) + 1
        val updatedOrder = IntArray().apply {
            existingPages.forEach { add(it.screenId) }
            if (!existingPages.any { it.screenId == newScreenId }) add(newScreenId)
        }

        val deferred = CompletableDeferred<Unit>()
        model.enqueueModelUpdateTask { _, _, _ ->
            val writer: ModelWriter = model.getWriter(false, CellPosMapper.DEFAULT, null)
            writer.persistWorkspaceScreenOrderSync(updatedOrder)

            if (groupIntoFolder || apps.size > maxSlots) {
                val folder = FolderInfo().apply { title = spaceName }
                writer.addItemToDatabase(folder, Favorites.CONTAINER_DESKTOP, newScreenId, 0, 0)
                apps.forEachIndexed { index, app ->
                    val item = app.makeWorkspaceItem(context)
                    item.rank = index
                    writer.addItemToDatabase(item, folder.id, 0, index % 4, index / 4)
                }
            } else {
                apps.take(maxSlots).forEachIndexed { index, app ->
                    val cellX = index % cols
                    val cellY = index / cols
                    val item = app.makeWorkspaceItem(context)
                    writer.addItemToDatabase(item, Favorites.CONTAINER_DESKTOP, newScreenId, cellX, cellY)
                }
            }
            deferred.complete(Unit)
        }
        deferred.await()

        withContext(Dispatchers.Main) {
            model.forceReload()
        }
        return newScreenId
    }

    fun getSavedScreenOrder(context: Context): String = LauncherPrefs.get(context).get(LauncherPrefs.WORKSPACE_SCREEN_ORDER)
}
