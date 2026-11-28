package app.mellune.homescreenfiles

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import app.mellune.util.FileAccessManager
import app.mellune.util.createSmallSearchResultBitmap
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.R
import com.android.launcher3.Workspace
import com.android.launcher3.celllayout.CellPosMapper
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.Themes
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeScreenFileEntry(
    val id: Long,
    val displayName: String,
    val mimeType: String,
    val contentUri: Uri,
)

/**
 * Bridges Pixel Launcher 17's "Home screen files" workflow cleanly to non-root Android:
 * scans MediaStore items placed inside a `Home screen` folder and syncs them as native
 * shortcuts into a dedicated home screen folder.
 */
object HomeScreenFilesManager {

    private const val FOLDER_QUERY_TOKEN = "%Home screen%"
    private const val MAX_SYNCED_FILES = 32

    suspend fun syncFilesToWorkspace(context: Context): Int = withContext(Dispatchers.IO) {
        val access = FileAccessManager.getInstance(context)
        if (!access.hasAnyPermission.value) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    R.string.home_screen_files_permission_needed,
                    Toast.LENGTH_LONG,
                ).show()
            }
            return@withContext 0
        }

        val entries = queryMediaStoreHomeScreenFiles(context)
        if (entries.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.home_screen_files_empty, Toast.LENGTH_SHORT).show()
            }
            return@withContext 0
        }

        val folderName = context.getString(R.string.home_screen_files_folder_name)
        val model = LauncherAppState.getInstance(context).model
        val future = CompletableFuture<Int>()

        model.enqueueModelUpdateTask { _, dataModel, _ ->
            val writer = model.getWriter(false, CellPosMapper.DEFAULT, null)
            val existingFolder = findExistingFolder(dataModel, folderName)
            val folder = existingFolder ?: FolderInfo().apply {
                title = folderName
                val screens = dataModel.itemsIdMap.collectWorkspaceScreens(context)
                val targetScreen = if (screens.isEmpty) Workspace.FIRST_SCREEN_ID else screens.get(screens.size() - 1)
                writer.addItemToDatabase(this, Favorites.CONTAINER_DESKTOP, targetScreen, 0, 0)
            }

            val existingUris = folder.getContents()
                .mapNotNull { it.getIntent()?.data?.toString() }
                .toHashSet()
            val accent = Themes.getColorAccent(context)
            val shortcutBitmap = BitmapInfo.of(
                createSmallSearchResultBitmap(context, R.drawable.ic_folder),
                accent,
            )

            var added = 0
            entries.forEach { file ->
                if (existingUris.contains(file.contentUri.toString())) return@forEach
                val viewIntent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(file.contentUri, file.mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

                val shortcut = WorkspaceItemInfo().apply {
                    itemType = Favorites.ITEM_TYPE_SHORTCUT
                    title = file.displayName
                    intent = viewIntent
                    bitmap = shortcutBitmap
                    rank = folder.getContents().size + added
                }
                writer.addItemToDatabase(shortcut, folder.id, 0, shortcut.rank % 4, shortcut.rank / 4)
                added++
            }
            future.complete(added)
        }

        val addedCount = future.get()
        withContext(Dispatchers.Main) {
            if (addedCount > 0) {
                model.forceReload()
            }
            val displayCount = if (addedCount > 0) addedCount else entries.size
            Toast.makeText(
                context,
                context.getString(R.string.home_screen_files_synced, displayCount),
                Toast.LENGTH_SHORT,
            ).show()
        }
        addedCount
    }

    private fun findExistingFolder(dataModel: BgDataModel, folderName: String): FolderInfo? {
        synchronized(dataModel) {
            dataModel.itemsIdMap.forEach { item ->
                if (item is FolderInfo && item.title?.toString() == folderName) {
                    return item
                }
            }
        }
        return null
    }

    private fun queryMediaStoreHomeScreenFiles(context: Context): List<HomeScreenFileEntry> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(FOLDER_QUERY_TOKEN)
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val results = mutableListOf<HomeScreenFileEntry>()
        runCatching {
            context.contentResolver.query(uri, projection, selection, args, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                while (cursor.moveToNext() && results.size < MAX_SYNCED_FILES) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: "*/*"
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    results.add(HomeScreenFileEntry(id, name, mime, contentUri))
                }
            }
        }
        return results
    }
}
