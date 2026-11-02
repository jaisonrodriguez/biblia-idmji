package org.idmji.biblia.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.idmji.biblia.model.Book
import org.idmji.biblia.model.CompendioItem
import org.idmji.biblia.model.CrossReference
import org.idmji.biblia.model.DictionaryEntry
import org.idmji.biblia.model.DictionaryLetter
import org.idmji.biblia.model.Pericopa
import org.idmji.biblia.model.ReadingSettings
import org.idmji.biblia.model.ReadingTheme
import org.idmji.biblia.model.ResolvedReference
import org.idmji.biblia.model.SearchResult
import org.idmji.biblia.model.Verse

class BibleRepository(context: Context) {
    private val dbHelper = BibleDatabaseHelper.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("biblia_idmji_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReadingSettings> = _settings.asStateFlow()

    private fun loadSettings(): ReadingSettings {
        val fontSize = prefs.getFloat("font_size", 18f)
        val themeName = prefs.getString("theme", ReadingTheme.SEPIA.name) ?: ReadingTheme.SEPIA.name
        val theme = try { ReadingTheme.valueOf(themeName) } catch (_: Exception) { ReadingTheme.SEPIA }
        val isSerif = prefs.getBoolean("is_serif", true)
        val isVerseByVerse = prefs.getBoolean("is_verse_by_verse", true)
        return ReadingSettings(
            fontSizeSp = fontSize,
            theme = theme,
            isSerif = isSerif,
            isVerseByVerse = isVerseByVerse
        )
    }

    fun updateSettings(newSettings: ReadingSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putFloat("font_size", newSettings.fontSizeSp)
            .putString("theme", newSettings.theme.name)
            .putBoolean("is_serif", newSettings.isSerif)
            .putBoolean("is_verse_by_verse", newSettings.isVerseByVerse)
            .apply()
    }

    suspend fun getBooks(): List<Book> = dbHelper.getBooks()

    suspend fun getVerses(bookId: Int, chapter: Int): List<Verse> = dbHelper.getVerses(bookId, chapter)

    suspend fun searchVerses(query: String): List<SearchResult> = dbHelper.searchVerses(query)

    suspend fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorHex: String?) {
        dbHelper.setHighlight(bookId, chapter, verse, colorHex)
    }

    suspend fun saveNote(bookId: Int, chapter: Int, verse: Int, note: String) {
        dbHelper.saveNote(bookId, chapter, verse, note)
    }

    suspend fun saveLastPosition(bookId: Int, chapter: Int, verse: Int) {
        dbHelper.saveLastPosition(bookId, chapter, verse)
    }

    suspend fun getLastPosition(): Triple<Int, Int, Int> = dbHelper.getLastPosition()

    suspend fun getDictionaryLetters(): List<DictionaryLetter> = dbHelper.getDictionaryLetters()

    suspend fun getDictionaryEntries(letter: String? = null, query: String? = null, limit: Int = 500): List<DictionaryEntry> =
        dbHelper.getDictionaryEntries(letter, query, limit)

    suspend fun getDictionaryEntry(id: Int): DictionaryEntry? = dbHelper.getDictionaryEntry(id)

    suspend fun getDictionaryEntriesForWords(words: List<String>): List<DictionaryEntry> =
        dbHelper.getDictionaryEntriesForWords(words)

    suspend fun getPericopasForChapter(bookId: Int, chapter: Int): Map<Int, Pericopa> =
        dbHelper.getPericopasForChapter(bookId, chapter)

    suspend fun getOfficialNotesForChapter(bookId: Int, chapter: Int): Map<Int, String> =
        dbHelper.getOfficialNotesForChapter(bookId, chapter)

    suspend fun getOfficialNoteForVerse(bookId: Int, chapter: Int, verse: Int): String? =
        dbHelper.getOfficialNoteForVerse(bookId, chapter, verse)

    suspend fun getPericopaForVerse(bookId: Int, chapter: Int, verse: Int): Pericopa? =
        dbHelper.getPericopaForVerse(bookId, chapter, verse)

    suspend fun getTskReferencesForVerse(bookId: Int, chapter: Int, verse: Int, limit: Int = 15): List<CrossReference> =
        dbHelper.getTskReferencesForVerse(bookId, chapter, verse, limit)

    suspend fun parseAndResolveOfficialNote(noteText: String): List<ResolvedReference> =
        dbHelper.parseAndResolveOfficialNote(noteText)

    fun getBookName(bookId: Int): String = dbHelper.getBookName(bookId)

    suspend fun getCompendioForVerse(bookId: Int, chapter: Int, verse: Int): List<CompendioItem> =
        dbHelper.getCompendioForVerse(bookId, chapter, verse)

    suspend fun getCompendioVersesForChapter(bookId: Int, chapter: Int): Set<Int> =
        dbHelper.getCompendioVersesForChapter(bookId, chapter)

    suspend fun getCanonicalBooksWithCompendio(): List<Pair<Book, Int>> =
        dbHelper.getCanonicalBooksWithCompendio()

    suspend fun getCompendioForBook(bookId: Int): List<CompendioItem> =
        dbHelper.getCompendioForBook(bookId)

    suspend fun getThematicCompendio(query: String = ""): List<CompendioItem> =
        dbHelper.getThematicCompendio(query)

    suspend fun searchCompendioGlobal(query: String): List<CompendioItem> =
        dbHelper.searchCompendioGlobal(query)

    suspend fun getCompendioItemDetail(id: Int): CompendioItem? =
        dbHelper.getCompendioItemDetail(id)
}
