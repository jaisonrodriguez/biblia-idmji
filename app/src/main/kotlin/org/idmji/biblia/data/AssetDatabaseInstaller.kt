package org.idmji.biblia.data

import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream

/** Runs before the helper opens a connection, on the repository's IO dispatcher. */
internal object AssetDatabaseInstaller {
    fun install(file: File, version: Int, copyAsset: (File) -> Unit) {
        file.parentFile?.mkdirs()
        val backup = File(file.path + ".previous")
        val pending = File(file.path + ".installing")
        // Recover a process interruption between the two renames.
        if (backup.exists()) {
            if (!file.exists()) rename(backup, file)
            else {
                open(file).use {
                    check(hasContentTables(it)) { "La instalación anterior está incompleta; se conserva su respaldo" }
                    checkIntegrity(it)
                }
                check(backup.delete()) { "No se pudo retirar el respaldo anterior" }
            }
        }
        if (file.exists()) {
            open(file).use { old ->
                check(old.version <= version) { "Base de datos de una versión más reciente; se conserva intacta" }
                if (old.version == version && hasContentTables(old)) return
            }
        }
        check(!pending.exists() || SQLiteDatabase.deleteDatabase(pending)) { "No se pudo retirar la copia temporal" }
        try {
            copyAsset(pending)
            open(pending).use { checkDatabase(it, version) }
            if (file.exists()) {
                // Consolidate WAL and remove its sidecars before replacing the closed DB.
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
                    old.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use {
                        check(it.moveToFirst() && it.getInt(0) == 0) { "La base de datos está ocupada" }
                    }
                    old.rawQuery("PRAGMA journal_mode=DELETE", null).use {
                        check(it.moveToFirst() && it.getString(0).equals("delete", true))
                    }
                    checkIntegrity(old)
                }
                SQLiteDatabase.openDatabase(pending.path, null, SQLiteDatabase.OPEN_READWRITE).use { fresh ->
                    fresh.execSQL("ATTACH DATABASE ? AS previous", arrayOf(file.path))
                    try {
                        fresh.beginTransaction()
                        try {
                            for (table in listOf("marcadores", "historial")) {
                                val columns = fresh.rawQuery("PRAGMA previous.table_info($table)", null).use { cursor ->
                                    buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
                                }
                                check(columns.isNotEmpty()) { "Falta la tabla de usuario $table; se conserva el original" }
                                val names = columns.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                                fresh.execSQL("DELETE FROM main.$table")
                                // Explicit columns preserve IDs, nulls, notes and original dates.
                                // Unknown/incompatible columns fail instead of silently losing data.
                                fresh.execSQL("INSERT INTO main.$table ($names) SELECT $names FROM previous.$table")
                                for ((a, b) in listOf("main" to "previous", "previous" to "main")) {
                                    fresh.rawQuery("SELECT $names FROM $a.$table EXCEPT SELECT $names FROM $b.$table", null).use {
                                        check(!it.moveToFirst()) { "Falló la verificación de $table" }
                                    }
                                }
                            }
                            fresh.setTransactionSuccessful()
                        } finally {
                            fresh.endTransaction()
                        }
                    } finally {
                        fresh.execSQL("DETACH DATABASE previous")
                    }
                    checkDatabase(fresh, version)
                }
            }
            FileOutputStream(pending, true).use { it.fd.sync() }
            if (file.exists()) rename(file, backup)
            // Until this succeeds, the previous file remains recoverable on next startup.
            rename(pending, file)
        } finally {
            SQLiteDatabase.deleteDatabase(pending)
        }
    }

    private fun rename(from: File, to: File) {
        Os.rename(from.path, to.path)
        val directory = Os.open(to.parentFile!!.path, OsConstants.O_RDONLY, 0)
        try { Os.fsync(directory) } finally { Os.close(directory) }
    }

    private fun open(file: File) = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)

    private fun hasContentTables(db: SQLiteDatabase): Boolean = db.rawQuery(
        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('referencias_cruzadas','compendio_idmji')", null
    ).use { it.moveToFirst() && it.getInt(0) == 2 }

    private fun checkDatabase(db: SQLiteDatabase, version: Int) {
        check(db.version == version) { "La versión del asset no coincide con la aplicación" }
        check(hasContentTables(db)) { "Faltan tablas de contenido" }
        checkIntegrity(db)
    }

    private fun checkIntegrity(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA integrity_check", null).use {
            check(it.moveToFirst() && it.getString(0) == "ok" && !it.moveToNext()) { "Base de datos dañada; se conserva el original" }
        }
    }
}
