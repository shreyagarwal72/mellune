package app.mellune.backup

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.model.DatabaseHelper
import com.android.launcher3.model.ModelDbController
import com.android.launcher3.pm.UserCache
import com.android.launcher3.provider.RestoreDbTask
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports home screen layouts (apps, folders, widgets, dock items) from OEM/system launchers
 * such as Oplus Launcher (ColorOS / OxygenOS / Realme UI), One UI, or Pixel Launcher—either
 * directly via their exported ContentProvider when permitted, or from an exported `.db` / `.zip`
 * backup file.
 */
class SystemLauncherImporter(private val context: Context) {

    data class ImportedItem(
        val id: Int,
        val container: Int,
        val itemType: Int,
        val title: String?,
        val intent: String?,
        val screen: Int,
        val cellX: Int,
        val cellY: Int,
        val spanX: Int,
        val spanY: Int,
        val appWidgetProvider: String?,
        val rank: Int,
    )

    data class ScanResult(
        val sourceName: String,
        val packageName: String?,
        val items: List<ImportedItem>,
        val errorMessage: String? = null,
    ) {
        val appCount: Int get() = items.count { it.itemType == Favorites.ITEM_TYPE_APPLICATION }
        val folderCount: Int get() = items.count { it.itemType == Favorites.ITEM_TYPE_FOLDER }
        val widgetCount: Int get() = items.count { it.itemType == Favorites.ITEM_TYPE_APPWIDGET }
    }

    suspend fun scanLiveSystemLauncher(): ScanResult = withContext(Dispatchers.IO) {
        val detectedPkg = detectSystemLauncherPackage()
        val sourceLabel = detectedPkg?.let { resolveAppLabel(it) } ?: "System Launcher"

        for (authority in SYSTEM_AUTHORITIES) {
            val uri = Uri.parse("content://$authority/favorites")
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val items = readCursor(cursor)
                    if (items.isNotEmpty()) {
                        return@withContext ScanResult(
                            sourceName = sourceLabel,
                            packageName = detectedPkg,
                            items = items,
                        )
                    }
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Permission denied querying $uri", se)
            } catch (t: Throwable) {
                Log.d(TAG, "Provider $uri unavailable: ${t.message}")
            }
        }

        ScanResult(
            sourceName = sourceLabel,
            packageName = detectedPkg,
            items = emptyList(),
            errorMessage = "Live provider protected by Android system permissions. " +
                "Pick an exported launcher .db or .zip backup file instead.",
        )
    }

    suspend fun scanBackupFile(uri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "sys_import_${UUID.randomUUID()}")
        tempDir.mkdirs()
        try {
            val stagedInput = File(tempDir, "input.bin")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stagedInput).use { output -> input.copyTo(output) }
            } ?: return@withContext ScanResult(
                sourceName = "Backup file",
                packageName = null,
                items = emptyList(),
                errorMessage = "Unable to open selected file",
            )

            val dbFile = extractDatabaseIfZip(stagedInput, tempDir) ?: stagedInput
            val items = readSqliteDatabase(dbFile)
            if (items.isEmpty()) {
                ScanResult(
                    sourceName = "Backup file",
                    packageName = null,
                    items = emptyList(),
                    errorMessage = "No launcher favorites table found in selected file",
                )
            } else {
                ScanResult(
                    sourceName = "System Launcher Backup",
                    packageName = null,
                    items = items,
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to parse system launcher backup", t)
            ScanResult(
                sourceName = "Backup file",
                packageName = null,
                items = emptyList(),
                errorMessage = t.message ?: "Failed to read backup file",
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun restoreItems(items: List<ImportedItem>) = withContext(Dispatchers.IO) {
        require(items.isNotEmpty()) { "No items to import" }
        val tempDir = File(context.cacheDir, "sys_restore_${UUID.randomUUID()}")
        tempDir.mkdirs()
        try {
            val stagedDbFile = File(tempDir, "staged_workspace.db")
            writeToStagedDatabase(items, stagedDbFile)

            val restoredDbFile = context.getDatabasePath(MelluneBackup.RESTORED_DB_FILE_NAME)
            restoredDbFile.parentFile?.mkdirs()
            stagedDbFile.copyTo(restoredDbFile, overwrite = true)

            val dbController = ModelDbController(context)
            RestoreDbTask.performRestore(context, dbController)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun extractDatabaseIfZip(inputFile: File, tempDir: File): File? {
        return try {
            FileInputStream(inputFile).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val lower = entry.name.lowercase()
                        if (!entry.isDirectory && (lower.endsWith(".db") || lower.endsWith(".sqlite"))) {
                            val out = File(tempDir, "extracted.db")
                            FileOutputStream(out).use { fos -> zis.copyTo(fos) }
                            return out
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun readSqliteDatabase(dbFile: File): List<ImportedItem> {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val tableName = findFavoritesTable(db) ?: return emptyList()
            db.rawQuery("SELECT * FROM $tableName", null).use { cursor ->
                return readCursor(cursor)
            }
        }
    }

    private fun findFavoritesTable(db: SQLiteDatabase): String? {
        val candidates = listOf("favorites", "singledesktopitems", "desktop", "workspace")
        val tables = mutableListOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) {
                tables += c.getString(0)
            }
        }
        return candidates.firstOrNull { cand -> tables.any { it.equals(cand, ignoreCase = true) } }
            ?: tables.firstOrNull { it.contains("favorite", ignoreCase = true) }
    }

    private fun readCursor(cursor: Cursor): List<ImportedItem> {
        val result = mutableListOf<ImportedItem>()
        while (cursor.moveToNext()) {
            val id = getIntOrDefault(cursor, "_id", result.size + 1)
            val container = getIntOrDefault(cursor, "container", Favorites.CONTAINER_DESKTOP)
            val itemType = getIntOrDefault(cursor, "itemType", Favorites.ITEM_TYPE_APPLICATION)
            val title = getStringOrNull(cursor, "title")
            val intent = getStringOrNull(cursor, "intent")
            val screen = getIntOrDefault(cursor, "screen", 0)
            val cellX = getIntOrDefault(cursor, "cellX", 0)
            val cellY = getIntOrDefault(cursor, "cellY", 0)
            val spanX = getIntOrDefault(cursor, "spanX", 1).coerceAtLeast(1)
            val spanY = getIntOrDefault(cursor, "spanY", 1).coerceAtLeast(1)
            val provider = getStringOrNull(cursor, "appWidgetProvider")
            val rank = getIntOrDefault(cursor, "rank", 0)

            if (itemType == Favorites.ITEM_TYPE_APPLICATION && intent.isNullOrBlank()) continue
            result += ImportedItem(
                id = id,
                container = container,
                itemType = itemType,
                title = title,
                intent = intent,
                screen = screen,
                cellX = cellX,
                cellY = cellY,
                spanX = spanX,
                spanY = spanY,
                appWidgetProvider = provider,
                rank = rank,
            )
        }
        return result
    }

    private fun writeToStagedDatabase(items: List<ImportedItem>, stagedDbFile: File) {
        val profileId = UserCache.INSTANCE.get(context).getSerialNumberForUser(android.os.Process.myUserHandle())
        val helper = DatabaseHelper(context, stagedDbFile.absolutePath, { profileId }, {})
        val now = System.currentTimeMillis()
        helper.writableDatabase.use { db ->
            db.execSQL("DELETE FROM ${Favorites.TABLE_NAME}")
            for (item in items) {
                val isHotseat = item.container == Favorites.CONTAINER_HOTSEAT
                db.execSQL(
                    """
                    INSERT INTO ${Favorites.TABLE_NAME} (
                        _id, title, intent, container, screen, cellX, cellY,
                        spanX, spanY, itemType, appWidgetProvider, modified, profileId, rank
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        item.id,
                        item.title,
                        item.intent,
                        item.container,
                        item.screen,
                        item.cellX,
                        if (isHotseat) 0 else item.cellY,
                        item.spanX,
                        item.spanY,
                        item.itemType,
                        item.appWidgetProvider,
                        now,
                        profileId,
                        item.rank,
                    ),
                )
            }
        }
        helper.close()
    }

    private fun detectSystemLauncherPackage(): String? {
        val pm = context.packageManager
        return SYSTEM_LAUNCHER_PACKAGES.firstOrNull { pkg ->
            runCatching {
                pm.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }
    }

    private fun resolveAppLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun getIntOrDefault(cursor: Cursor, col: String, default: Int): Int {
        val idx = cursor.getColumnIndex(col)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getInt(idx) else default
    }

    private fun getStringOrNull(cursor: Cursor, col: String): String? {
        val idx = cursor.getColumnIndex(col)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
    }

    companion object {
        private const val TAG = "SystemLauncherImporter"

        private val SYSTEM_LAUNCHER_PACKAGES = listOf(
            "com.android.launcher", // Oplus / ColorOS / OxygenOS
            "com.oplus.launcher",
            "com.oppo.launcher",
            "com.oneplus.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
        )

        private val SYSTEM_AUTHORITIES = listOf(
            "com.android.launcher.settings",
            "com.oplus.launcher.settings",
            "com.oppo.launcher.settings",
            "com.oneplus.launcher.settings",
            "com.google.android.apps.nexuslauncher.settings",
            "com.sec.android.app.launcher.settings",
            "com.android.launcher3.settings",
        )
    }
}
