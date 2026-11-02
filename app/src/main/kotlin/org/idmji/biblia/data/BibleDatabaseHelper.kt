package org.idmji.biblia.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.idmji.biblia.model.Book
import org.idmji.biblia.model.CompendioItem
import org.idmji.biblia.model.CrossReference
import org.idmji.biblia.model.DictionaryEntry
import org.idmji.biblia.model.DictionaryLetter
import org.idmji.biblia.model.Pericopa
import org.idmji.biblia.model.ResolvedReference
import org.idmji.biblia.model.SearchResult
import org.idmji.biblia.model.Verse
import java.io.FileOutputStream
import java.text.Normalizer

class BibleDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "biblia_rvr1960.db"
        private const val DB_VERSION = 2

        // Orden canónico según Manual para Leer la Biblia de la IDMJI (Columna derecha)
        val NT_IDMJI_ORDER = listOf(
            "Mateo", "Marcos", "Lucas", "Juan", "Hechos",
            "Romanos", "1 Corintios", "2 Corintios", "Gálatas", "Efesios", "Filipenses", "Colosenses",
            "1 Tesalonicenses", "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", "Tito", "Filemón",
            "Hebreos", "Santiago", "1 Pedro", "2 Pedro",
            "Judas", "1 Juan", "Apocalipsis", "2 Juan", "3 Juan"
        )
        private val NT_ORDER_MAP = NT_IDMJI_ORDER.mapIndexed { index, name -> name to index }.toMap()

        @Volatile
        private var instance: BibleDatabaseHelper? = null

        val SBU_ABBR_MAP = mapOf(
            "gn" to 1, "ex" to 2, "éx" to 2, "lv" to 3, "nm" to 4, "dt" to 5,
            "jos" to 6, "jue" to 7, "rt" to 8,
            "1 s" to 9, "1s" to 9, "2 s" to 10, "2s" to 10,
            "1 r" to 11, "1r" to 11, "2 r" to 12, "2r" to 12,
            "1 cr" to 13, "1cr" to 13, "2 cr" to 14, "2cr" to 14,
            "esd" to 15, "neh" to 16, "est" to 17, "job" to 18,
            "sal" to 19, "pr" to 20, "ec" to 21, "cant" to 22,
            "is" to 23, "jer" to 24, "lam" to 25, "ez" to 26, "dn" to 27,
            "os" to 28, "jl" to 29, "am" to 30, "abd" to 31, "jon" to 32,
            "miq" to 33, "nah" to 34, "hab" to 35, "sof" to 36, "hag" to 37,
            "zac" to 38, "mal" to 39,
            "mt" to 40, "mr" to 41, "mc" to 41, "lc" to 42, "jn" to 43,
            "hch" to 44, "ro" to 45,
            "1 co" to 46, "1co" to 46, "2 co" to 47, "2co" to 47,
            "gá" to 48, "ga" to 48, "ef" to 49, "fil" to 50, "col" to 51,
            "1 ts" to 52, "1ts" to 52, "2 ts" to 53, "2ts" to 53,
            "1 ti" to 54, "1ti" to 54, "2 ti" to 55, "2ti" to 55,
            "tit" to 56, "flm" to 57, "he" to 58, "stg" to 59,
            "1 p" to 60, "1p" to 60, "2 p" to 61, "2p" to 61,
            "1 jn" to 62, "1jn" to 62, "2 jn" to 63, "2jn" to 63, "3 jn" to 64, "3jn" to 64,
            "jud" to 65, "ap" to 66
        )

        fun getInstance(context: Context): BibleDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: BibleDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        ensureDatabaseCopied()
    }

    private fun normalizeForSearch(text: String): String {
        return Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun ensureDatabaseCopied() {
        AssetDatabaseInstaller.install(context.getDatabasePath(DB_NAME), DB_VERSION) { destination ->
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("La actualización debe completarse antes de abrir la base de datos")
    }

    suspend fun getBooks(): List<Book> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, nombre, abreviatura, testamento, categoria, num_capitulos FROM libros ORDER BY id ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Book(
                        id = it.getInt(0),
                        name = it.getString(1),
                        abbreviation = it.getString(2),
                        testament = it.getString(3),
                        category = it.getString(4),
                        chaptersCount = it.getInt(5)
                    )
                )
            }
        }
        val atBooks = list.filter { it.testament == "AT" }.sortedBy { it.id }
        val ntBooks = list.filter { it.testament == "NT" }.sortedBy { NT_ORDER_MAP[it.name] ?: 999 }
        atBooks + ntBooks
    }

    suspend fun getVerses(bookId: Int, chapter: Int): List<Verse> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Verse>()
        val db = readableDatabase
        val query = """
            SELECT v.id, v.libro_id, v.capitulo, v.versiculo, v.texto, m.color, m.nota
            FROM versiculos v
            LEFT JOIN marcadores m ON v.libro_id = m.libro_id AND v.capitulo = m.capitulo AND v.versiculo = m.versiculo
            WHERE v.libro_id = ? AND v.capitulo = ?
            ORDER BY v.versiculo ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(bookId.toString(), chapter.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Verse(
                        id = it.getInt(0),
                        bookId = it.getInt(1),
                        chapter = it.getInt(2),
                        verse = it.getInt(3),
                        text = it.getString(4),
                        highlightColor = it.getString(5),
                        note = it.getString(6)
                    )
                )
            }
        }
        list
    }

    suspend fun searchVerses(queryText: String, limit: Int = 100): List<SearchResult> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchResult>()
        if (queryText.trim().isEmpty()) return@withContext list

        val db = readableDatabase
        val sql = """
            SELECT v.id, v.libro_id, l.nombre, v.capitulo, v.versiculo, v.texto
            FROM versiculos v
            JOIN libros l ON v.libro_id = l.id
            WHERE v.texto LIKE ?
            ORDER BY v.libro_id ASC, v.capitulo ASC, v.versiculo ASC
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf("%${queryText.trim()}%", limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SearchResult(
                        verseId = it.getInt(0),
                        bookId = it.getInt(1),
                        bookName = it.getString(2),
                        chapter = it.getInt(3),
                        verse = it.getInt(4),
                        text = it.getString(5)
                    )
                )
            }
        }
        list
    }

    suspend fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorHex: String?) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        if (colorHex == null) {
            // Eliminar resaltado si tampoco hay nota
            db.delete("marcadores", "libro_id = ? AND capitulo = ? AND versiculo = ? AND (nota IS NULL OR nota = '')", 
                arrayOf(bookId.toString(), chapter.toString(), verse.toString()))
            // Si hay nota, solo limpiar el color
            val cv = ContentValues().apply { putNull("color") }
            db.update("marcadores", cv, "libro_id = ? AND capitulo = ? AND versiculo = ?", 
                arrayOf(bookId.toString(), chapter.toString(), verse.toString()))
        } else {
            val cv = ContentValues().apply {
                put("libro_id", bookId)
                put("capitulo", chapter)
                put("versiculo", verse)
                put("color", colorHex)
            }
            db.insertWithOnConflict("marcadores", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    suspend fun saveNote(bookId: Int, chapter: Int, verse: Int, noteText: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val trimmed = noteText.trim()
        if (trimmed.isEmpty()) {
            db.delete("marcadores", "libro_id = ? AND capitulo = ? AND versiculo = ? AND color IS NULL", 
                arrayOf(bookId.toString(), chapter.toString(), verse.toString()))
            val cv = ContentValues().apply { putNull("nota") }
            db.update("marcadores", cv, "libro_id = ? AND capitulo = ? AND versiculo = ?", 
                arrayOf(bookId.toString(), chapter.toString(), verse.toString()))
        } else {
            val cv = ContentValues().apply {
                put("libro_id", bookId)
                put("capitulo", chapter)
                put("versiculo", verse)
                put("nota", trimmed)
            }
            db.insertWithOnConflict("marcadores", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    suspend fun saveLastPosition(bookId: Int, chapter: Int, verse: Int) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", 1)
            put("libro_id", bookId)
            put("capitulo", chapter)
            put("versiculo", verse)
        }
        db.insertWithOnConflict("historial", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getLastPosition(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT libro_id, capitulo, versiculo FROM historial WHERE id = 1", null)
        cursor.use {
            if (it.moveToFirst()) {
                Triple(it.getInt(0), it.getInt(1), it.getInt(2))
            } else {
                Triple(1, 1, 1) // Génesis 1:1 por defecto
            }
        }
    }

    suspend fun getDictionaryLetters(): List<DictionaryLetter> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DictionaryLetter>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT letra, COUNT(*) FROM diccionario GROUP BY letra ORDER BY letra ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(DictionaryLetter(it.getString(0), it.getInt(1)))
            }
        }
        list
    }

    suspend fun getDictionaryEntries(letter: String? = null, query: String? = null, limit: Int = 500): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DictionaryEntry>()
        val db = readableDatabase
        val qClean = if (!query.isNullOrBlank()) normalizeForSearch(query) else null

        val cursor = when {
            qClean != null -> {
                db.rawQuery(
                    "SELECT id, letra, termino, definicion FROM diccionario WHERE termino_limpio LIKE ? ORDER BY termino ASC LIMIT ?",
                    arrayOf("%$qClean%", limit.toString())
                )
            }
            !letter.isNullOrBlank() -> {
                db.rawQuery(
                    "SELECT id, letra, termino, definicion FROM diccionario WHERE letra = ? ORDER BY termino ASC LIMIT ?",
                    arrayOf(letter.uppercase(), limit.toString())
                )
            }
            else -> {
                db.rawQuery(
                    "SELECT id, letra, termino, definicion FROM diccionario ORDER BY termino ASC LIMIT ?",
                    arrayOf(limit.toString())
                )
            }
        }

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    DictionaryEntry(
                        id = it.getInt(0),
                        letter = it.getString(1),
                        term = it.getString(2),
                        definition = it.getString(3)
                    )
                )
            }
        }
        list
    }

    suspend fun getDictionaryEntry(id: Int): DictionaryEntry? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, letra, termino, definicion FROM diccionario WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                DictionaryEntry(
                    id = it.getInt(0),
                    letter = it.getString(1),
                    term = it.getString(2),
                    definition = it.getString(3)
                )
            } else null
        }
    }

    suspend fun getDictionaryEntriesForWords(words: List<String>): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DictionaryEntry>()
        if (words.isEmpty()) return@withContext list
        val db = readableDatabase

        val cleanWords = mutableSetOf<String>()
        for (w in words) {
            val cl = normalizeForSearch(w)
            if (cl.length >= 3) {
                cleanWords.add(cl)
                if (cl.endsWith("es")) cleanWords.add(cl.dropLast(2))
                else if (cl.endsWith("s")) cleanWords.add(cl.dropLast(1))
            }
        }
        if (cleanWords.isEmpty()) return@withContext list

        val placeholders = cleanWords.joinToString(",") { "?" }
        val query = "SELECT id, letra, termino, definicion FROM diccionario WHERE termino_limpio IN ($placeholders) ORDER BY termino ASC"
        val cursor = db.rawQuery(query, cleanWords.toTypedArray())
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    DictionaryEntry(
                        id = it.getInt(0),
                        letter = it.getString(1),
                        term = it.getString(2),
                        definition = it.getString(3)
                    )
                )
            }
        }
        list
    }

    // ==========================================
    // MÉTODOS DE REFERENCIAS CRUZADAS Y PERÍCOPES
    // ==========================================

    suspend fun getPericopasForChapter(bookId: Int, chapter: Int): Map<Int, Pericopa> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Int, Pericopa>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, libro_id, capitulo, versiculo, titulo, pasajes_paralelos FROM pericopas WHERE libro_id = ? AND capitulo = ?",
            arrayOf(bookId.toString(), chapter.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val p = Pericopa(
                    id = it.getLong(0),
                    bookId = it.getInt(1),
                    chapter = it.getInt(2),
                    verse = it.getInt(3),
                    title = it.getString(4),
                    parallelPassages = if (it.isNull(5)) null else it.getString(5)
                )
                map[p.verse] = p
            }
        }
        map
    }

    suspend fun getOfficialNotesForChapter(bookId: Int, chapter: Int): Map<Int, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Int, String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT versiculo, nota FROM referencias_rvr1960 WHERE libro_id = ? AND capitulo = ?",
            arrayOf(bookId.toString(), chapter.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                map[it.getInt(0)] = it.getString(1)
            }
        }
        map
    }

    suspend fun getOfficialNoteForVerse(bookId: Int, chapter: Int, verse: Int): String? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT nota FROM referencias_rvr1960 WHERE libro_id = ? AND capitulo = ? AND versiculo = ?",
            arrayOf(bookId.toString(), chapter.toString(), verse.toString())
        )
        cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    suspend fun getPericopaForVerse(bookId: Int, chapter: Int, verse: Int): Pericopa? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, libro_id, capitulo, versiculo, titulo, pasajes_paralelos FROM pericopas WHERE libro_id = ? AND capitulo = ? AND versiculo <= ? ORDER BY versiculo DESC LIMIT 1",
            arrayOf(bookId.toString(), chapter.toString(), verse.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                Pericopa(
                    id = it.getLong(0),
                    bookId = it.getInt(1),
                    chapter = it.getInt(2),
                    verse = it.getInt(3),
                    title = it.getString(4),
                    parallelPassages = if (it.isNull(5)) null else it.getString(5)
                )
            } else null
        }
    }

    suspend fun getTskReferencesForVerse(bookId: Int, chapter: Int, verse: Int, limit: Int = 15): List<CrossReference> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CrossReference>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT rc.id, rc.origen_libro_id, rc.origen_capitulo, rc.origen_versiculo,
                   rc.destino_libro_id, l.nombre, rc.destino_capitulo,
                   rc.destino_versiculo_inicio, rc.destino_versiculo_fin, rc.votos
            FROM referencias_cruzadas rc
            JOIN libros l ON l.id = rc.destino_libro_id
            WHERE rc.origen_libro_id = ? AND rc.origen_capitulo = ? AND rc.origen_versiculo = ?
            ORDER BY rc.votos DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(bookId.toString(), chapter.toString(), verse.toString(), limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val destBookId = it.getInt(4)
                val destChapter = it.getInt(6)
                val destStart = it.getInt(7)
                val destEnd = it.getInt(8)
                list.add(
                    CrossReference(
                        id = it.getLong(0),
                        originBookId = it.getInt(1),
                        originChapter = it.getInt(2),
                        originVerse = it.getInt(3),
                        destBookId = destBookId,
                        destBookName = it.getString(5),
                        destChapter = destChapter,
                        destVerseStart = destStart,
                        destVerseEnd = destEnd,
                        votes = it.getInt(9),
                        text = resolvePassageText(db, destBookId, destChapter, destStart, destEnd)
                    )
                )
            }
        }
        list
    }

    suspend fun parseAndResolveOfficialNote(noteText: String): List<ResolvedReference> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ResolvedReference>()
        val db = readableDatabase
        var currentBookId: Int? = null

        val parts = noteText.split(';').map { it.trim().trimEnd('.') }.filter { it.isNotEmpty() }
        val bookRegex = Regex("""^((?:[1-3]\s+)?[a-zA-ZáéíóúÁÉÍÓÚñÑ]+)\.?\s*(.*)$""")
        val cvRegex = Regex("""^(\d+)\.(\d+)(?:-(\d+))?""")

        for (part in parts) {
            val bookMatch = bookRegex.find(part)
            val rest = if (bookMatch != null && SBU_ABBR_MAP.containsKey(bookMatch.groupValues[1].lowercase())) {
                currentBookId = SBU_ABBR_MAP[bookMatch.groupValues[1].lowercase()]
                bookMatch.groupValues[2].trim()
            } else {
                part
            }

            val bId = currentBookId ?: continue
            val bName = getBookName(bId)

            val cvMatch = cvRegex.find(rest)
            if (cvMatch != null) {
                val chapter = cvMatch.groupValues[1].toIntOrNull() ?: continue
                val vStart = cvMatch.groupValues[2].toIntOrNull() ?: continue
                val vEnd = cvMatch.groupValues[3].toIntOrNull() ?: vStart
                val text = resolvePassageText(db, bId, chapter, vStart, vEnd)
                val citation = if (vStart == vEnd) "$bName $chapter:$vStart" else "$bName $chapter:$vStart-$vEnd"
                list.add(
                    ResolvedReference(
                        citation = citation,
                        bookId = bId,
                        bookName = bName,
                        chapter = chapter,
                        verseStart = vStart,
                        verseEnd = vEnd,
                        text = text
                    )
                )
            }
        }
        list
    }

    suspend fun getBookName(bookId: Int): String = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT nombre FROM libros WHERE id = ?", arrayOf(bookId.toString()))
        return@withContext cursor.use {
            if (it.moveToFirst()) it.getString(0) else "Libro $bookId"
        }
    }

    private fun resolvePassageText(db: SQLiteDatabase, bookId: Int, chapter: Int, startVerse: Int, endVerse: Int): String {
        val cursor = db.rawQuery(
            "SELECT versiculo, texto FROM versiculos WHERE libro_id = ? AND capitulo = ? AND versiculo >= ? AND versiculo <= ? ORDER BY versiculo",
            arrayOf(bookId.toString(), chapter.toString(), startVerse.toString(), endVerse.toString())
        )
        val sb = StringBuilder()
        cursor.use {
            while (it.moveToNext()) {
                val vNum = it.getInt(0)
                val text = it.getString(1)
                if (startVerse == endVerse) {
                    sb.append(text)
                } else {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append("[$vNum] $text")
                }
            }
        }
        return sb.toString().trim()
    }

    // ==========================================
    // COMPENDIO DOCTRINAL IDMJI
    // ==========================================

    private fun cursorToCompendioItem(cursor: android.database.Cursor): CompendioItem {
        return CompendioItem(
            id = cursor.getInt(0),
            tipo = cursor.getString(1),
            libroId = if (cursor.isNull(2)) null else cursor.getInt(2),
            libroNombre = if (cursor.isNull(3)) "" else cursor.getString(3),
            capitulo = if (cursor.isNull(4)) null else cursor.getInt(4),
            versiculoInicio = if (cursor.isNull(5)) null else cursor.getInt(5),
            versiculoFin = if (cursor.isNull(6)) null else cursor.getInt(6),
            cita = if (cursor.isNull(7)) "" else cursor.getString(7),
            titulo = cursor.getString(8),
            videoUrl = if (cursor.isNull(9)) "" else cursor.getString(9),
            videoId = if (cursor.isNull(10)) "" else cursor.getString(10),
            seccion = if (cursor.isNull(11)) "" else cursor.getString(11),
            transcripcion = if (cursor.isNull(12)) "" else cursor.getString(12)
        )
    }

    suspend fun getCompendioForVerse(bookId: Int, chapter: Int, verse: Int): List<CompendioItem> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT c.id, c.tipo, c.libro_id, l.nombre, c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, '' as transcripcion
            FROM compendio_idmji c
            LEFT JOIN libros l ON c.libro_id = l.id
            WHERE c.tipo = 'CANONICO' AND c.libro_id = ? AND c.capitulo = ? AND ? BETWEEN c.versiculo_inicio AND c.versiculo_fin
            ORDER BY c.id
            """.trimIndent(),
            arrayOf(bookId.toString(), chapter.toString(), verse.toString())
        )
        val list = mutableListOf<CompendioItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCompendioItem(it))
            }
        }
        return@withContext list
    }

    suspend fun getCompendioVersesForChapter(bookId: Int, chapter: Int): Set<Int> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT versiculo_inicio, versiculo_fin FROM compendio_idmji WHERE tipo = 'CANONICO' AND libro_id = ? AND capitulo = ?",
            arrayOf(bookId.toString(), chapter.toString())
        )
        val verseSet = mutableSetOf<Int>()
        cursor.use {
            while (it.moveToNext()) {
                val start = it.getInt(0)
                val end = it.getInt(1)
                if (end >= 999) {
                    val vCursor = db.rawQuery(
                        "SELECT MAX(versiculo) FROM versiculos WHERE libro_id = ? AND capitulo = ?",
                        arrayOf(bookId.toString(), chapter.toString())
                    )
                    val maxV = vCursor.use { vc -> if (vc.moveToFirst()) vc.getInt(0) else 50 }
                    for (v in 1..maxV) {
                        verseSet.add(v)
                    }
                } else {
                    for (v in start..end) {
                        verseSet.add(v)
                    }
                }
            }
        }
        return@withContext verseSet
    }

    suspend fun getCanonicalBooksWithCompendio(): List<Pair<Book, Int>> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT l.id, l.nombre, l.abreviatura, l.testamento, l.categoria, l.num_capitulos, COUNT(c.id) as count
            FROM libros l
            JOIN compendio_idmji c ON l.id = c.libro_id
            WHERE c.tipo = 'CANONICO'
            GROUP BY l.id
            ORDER BY l.id
            """.trimIndent(),
            null
        )
        val list = mutableListOf<Pair<Book, Int>>()
        cursor.use {
            while (it.moveToNext()) {
                val book = Book(
                    id = it.getInt(0),
                    name = it.getString(1),
                    abbreviation = it.getString(2),
                    testament = it.getString(3),
                    category = it.getString(4),
                    chaptersCount = it.getInt(5)
                )
                val count = it.getInt(6)
                list.add(Pair(book, count))
            }
        }
        return@withContext list
    }

    suspend fun getCompendioForBook(bookId: Int): List<CompendioItem> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT c.id, c.tipo, c.libro_id, l.nombre, c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, '' as transcripcion
            FROM compendio_idmji c
            LEFT JOIN libros l ON c.libro_id = l.id
            WHERE c.tipo = 'CANONICO' AND c.libro_id = ?
            ORDER BY c.capitulo ASC, c.versiculo_inicio ASC, c.id ASC
            """.trimIndent(),
            arrayOf(bookId.toString())
        )
        val list = mutableListOf<CompendioItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCompendioItem(it))
            }
        }
        return@withContext list
    }

    suspend fun getThematicCompendioCount(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM compendio_idmji WHERE tipo = 'TEMATICO'", null).use {
            check(it.moveToFirst())
            it.getInt(0)
        }
    }

    suspend fun getThematicCompendio(query: String = "", offset: Int = 0): List<CompendioItem> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val sql = if (query.isBlank()) {
            """
            SELECT c.id, c.tipo, c.libro_id, '', c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, '' as transcripcion
            FROM compendio_idmji c
            WHERE c.tipo = 'TEMATICO'
            ORDER BY c.id ASC
            LIMIT 200 OFFSET ?
            """.trimIndent()
        } else {
            """
            SELECT c.id, c.tipo, c.libro_id, '', c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, '' as transcripcion
            FROM compendio_idmji c
            WHERE c.tipo = 'TEMATICO' AND (c.titulo LIKE ? OR c.seccion LIKE ?)
            ORDER BY c.id ASC
            LIMIT 200 OFFSET ?
            """.trimIndent()
        }
        val args = if (query.isBlank()) arrayOf(offset.coerceAtLeast(0).toString()) else arrayOf("%$query%", "%$query%", offset.coerceAtLeast(0).toString())
        val cursor = db.rawQuery(sql, args)
        val list = mutableListOf<CompendioItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCompendioItem(it))
            }
        }
        return@withContext list
    }

    suspend fun searchCompendioGlobal(query: String): List<CompendioItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT c.id, c.tipo, c.libro_id, COALESCE(l.nombre, ''), c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, '' as transcripcion
            FROM compendio_idmji c
            LEFT JOIN libros l ON c.libro_id = l.id
            WHERE c.titulo LIKE ? OR c.cita LIKE ? OR c.seccion LIKE ?
            ORDER BY c.tipo ASC, c.id ASC
            LIMIT 100
            """.trimIndent(),
            arrayOf("%$query%", "%$query%", "%$query%")
        )
        val list = mutableListOf<CompendioItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCompendioItem(it))
            }
        }
        return@withContext list
    }

    suspend fun getCompendioItemDetail(id: Int): CompendioItem? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT c.id, c.tipo, c.libro_id, COALESCE(l.nombre, ''), c.capitulo, c.versiculo_inicio, c.versiculo_fin,
                   c.cita, c.titulo, c.video_url, c.video_id, c.seccion, c.transcripcion
            FROM compendio_idmji c
            LEFT JOIN libros l ON c.libro_id = l.id
            WHERE c.id = ?
            """.trimIndent(),
            arrayOf(id.toString())
        )
        return@withContext cursor.use {
            if (it.moveToFirst()) cursorToCompendioItem(it) else null
        }
    }
}
