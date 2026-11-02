package org.idmji.biblia.model

enum class ReadingTheme {
    DAY,
    SEPIA,
    NIGHT
}

data class Book(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val testament: String, // "AT" or "NT"
    val category: String,
    val chaptersCount: Int
)

data class Verse(
    val id: Int,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val highlightColor: String? = null,
    val note: String? = null
)

data class ReadingSettings(
    val fontSizeSp: Float = 18f,
    val theme: ReadingTheme = ReadingTheme.SEPIA,
    val isSerif: Boolean = true,
    val isVerseByVerse: Boolean = true
)

data class SearchResult(
    val verseId: Int,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class DictionaryLetter(
    val letter: String,
    val count: Int
)

data class DictionaryEntry(
    val id: Int,
    val letter: String,
    val term: String,
    val definition: String
)

data class Pericopa(
    val id: Long,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val title: String,
    val parallelPassages: String? = null
)

data class CrossReference(
    val id: Long,
    val originBookId: Int,
    val originChapter: Int,
    val originVerse: Int,
    val destBookId: Int,
    val destBookName: String,
    val destChapter: Int,
    val destVerseStart: Int,
    val destVerseEnd: Int,
    val votes: Int,
    val text: String = ""
)

data class ResolvedReference(
    val citation: String,
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val text: String
)

data class CompendioItem(
    val id: Int,
    val tipo: String, // "CANONICO" or "TEMATICO"
    val libroId: Int?,
    val libroNombre: String = "",
    val capitulo: Int?,
    val versiculoInicio: Int?,
    val versiculoFin: Int?,
    val cita: String,
    val titulo: String,
    val videoUrl: String,
    val videoId: String,
    val seccion: String,
    val transcripcion: String
)
