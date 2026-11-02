package org.idmji.biblia.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File

class AssetDatabaseInstallerTest {
    private lateinit var directory: File
    private lateinit var installed: File
    private lateinit var asset: File

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "migration-${System.nanoTime()}").apply { mkdirs() }
        installed = File(directory, "installed.db")
        asset = File(directory, "asset.db")
        create(asset, 2)
    }

    @After fun cleanup() { directory.deleteRecursively() }

    private fun create(file: File, version: Int) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL("CREATE TABLE marcadores(id INTEGER PRIMARY KEY AUTOINCREMENT,libro_id INTEGER,capitulo INTEGER,versiculo INTEGER,color TEXT,nota TEXT,fecha_creacion TEXT)")
            it.execSQL("CREATE TABLE historial(id INTEGER PRIMARY KEY,libro_id INTEGER,capitulo INTEGER,versiculo INTEGER,fecha_actualizacion TEXT)")
            it.execSQL("CREATE TABLE compendio_idmji(id INTEGER PRIMARY KEY)")
            it.execSQL("CREATE TABLE referencias_rvr1960(id INTEGER PRIMARY KEY)")
            it.version = version
        }
    }

    private fun install() = AssetDatabaseInstaller.install(installed, 2) { asset.copyTo(it, overwrite = true); Unit }

    @Test fun cleanInstallHasAssetVersion() {
        install()
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(2, it.version) }
    }

    @Test fun upgradePreservesIdsNullsNotesDatesAndHistory() {
        create(installed, 1)
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("INSERT INTO marcadores VALUES(17,40,9,22,NULL,'Nota de estudio','2020-01-02')")
            it.execSQL("INSERT INTO historial VALUES(1,41,10,52,'2021-03-04')")
        }
        install()
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(2, db.version)
            db.rawQuery("SELECT * FROM marcadores", null).use {
                assertTrue(it.moveToFirst()); assertEquals(17, it.getInt(0)); assertTrue(it.isNull(4))
                assertEquals("Nota de estudio", it.getString(5)); assertEquals("2020-01-02", it.getString(6))
                assertFalse(it.moveToNext())
            }
            db.rawQuery("SELECT * FROM historial", null).use {
                assertTrue(it.moveToFirst()); assertEquals(41, it.getInt(1)); assertEquals(52, it.getInt(3))
                assertEquals("2021-03-04", it.getString(4))
            }
        }
        assertTrue(File(installed.path + ".previous").exists())
    }

    @Test fun failedCopyKeepsOriginal() {
        create(installed, 1)
        val before = installed.readBytes()
        try {
            AssetDatabaseInstaller.install(installed, 2) { it.writeText("partial"); error("Copia interrumpida") }
            fail("Debe fallar")
        } catch (_: IllegalStateException) { }
        assertArrayEquals(before, installed.readBytes())
    }

    @Test fun incompatibleUserSchemaAbortsWithoutReplacingOriginal() {
        create(installed, 1)
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("ALTER TABLE marcadores ADD COLUMN important_user_data TEXT")
        }
        try { install(); fail("Debe conservar columnas desconocidas en el original") }
        catch (_: android.database.sqlite.SQLiteException) { }
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(1, it.version) }
    }

    @Test fun interruptedRenameRecoversPreviousDatabase() {
        create(installed, 1)
        assertTrue(installed.renameTo(File(installed.path + ".previous")))
        install()
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(2, it.version) }
    }

    @Test fun nextUpgradeWorksWithBackupFromPreviousSuccessfulUpgrade() {
        create(installed, 1)
        install()
        assertTrue(File(installed.path + ".previous").exists())
        SQLiteDatabase.openDatabase(asset.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 3 }
        AssetDatabaseInstaller.install(installed, 3) { asset.copyTo(it, overwrite = true); Unit }
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(3, it.version) }
    }

    @Test fun currentDatabaseDoesNotRecopyAssetAndFutureVersionIsRejected() {
        create(installed, 2)
        AssetDatabaseInstaller.install(installed, 2) { error("No debe copiar") }
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 3 }
        try { install(); fail("No debe degradar una versión nueva") }
        catch (_: IllegalStateException) { }
        SQLiteDatabase.openDatabase(installed.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(3, it.version) }
    }
}
