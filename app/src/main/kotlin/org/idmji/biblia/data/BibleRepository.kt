package org.idmji.biblia.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val appContext = context.applicationContext
    private val dbHelper by lazy { BibleDatabaseHelper.getInstance(appContext) }
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

    suspend fun getBooks(): List<Book> = withContext(Dispatchers.IO) { dbHelper.getBooks() }

    suspend fun getVerses(bookId: Int, chapter: Int): List<Verse> = withContext(Dispatchers.IO) { dbHelper.getVerses(bookId, chapter) }

    suspend fun searchVerses(query: String): List<SearchResult> = withContext(Dispatchers.IO) { dbHelper.searchVerses(query) }

    suspend fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorHex: String?) = withContext(Dispatchers.IO) {
        dbHelper.setHighlight(bookId, chapter, verse, colorHex)
    }

    suspend fun saveNote(bookId: Int, chapter: Int, verse: Int, note: String) = withContext(Dispatchers.IO) {
        dbHelper.saveNote(bookId, chapter, verse, note)
    }

    suspend fun saveLastPosition(bookId: Int, chapter: Int, verse: Int) = withContext(Dispatchers.IO) {
        dbHelper.saveLastPosition(bookId, chapter, verse)
    }

    suspend fun getLastPosition(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) { dbHelper.getLastPosition() }

    suspend fun getDictionaryLetters(): List<DictionaryLetter> = withContext(Dispatchers.IO) { dbHelper.getDictionaryLetters() }

    suspend fun getDictionaryEntries(letter: String? = null, query: String? = null, limit: Int = 500): List<DictionaryEntry> = withContext(Dispatchers.IO) { dbHelper.getDictionaryEntries(letter, query, limit) }

    suspend fun getDictionaryEntry(id: Int): DictionaryEntry? = withContext(Dispatchers.IO) { dbHelper.getDictionaryEntry(id) }

    suspend fun getDictionaryEntriesForWords(words: List<String>): List<DictionaryEntry> = withContext(Dispatchers.IO) { dbHelper.getDictionaryEntriesForWords(words) }

    suspend fun getPericopasForChapter(bookId: Int, chapter: Int): Map<Int, Pericopa> = withContext(Dispatchers.IO) { dbHelper.getPericopasForChapter(bookId, chapter) }

    suspend fun getOfficialNotesForChapter(bookId: Int, chapter: Int): Map<Int, String> = withContext(Dispatchers.IO) { dbHelper.getOfficialNotesForChapter(bookId, chapter) }

    suspend fun getOfficialNoteForVerse(bookId: Int, chapter: Int, verse: Int): String? = withContext(Dispatchers.IO) { dbHelper.getOfficialNoteForVerse(bookId, chapter, verse) }

    suspend fun getPericopaForVerse(bookId: Int, chapter: Int, verse: Int): Pericopa? = withContext(Dispatchers.IO) { dbHelper.getPericopaForVerse(bookId, chapter, verse) }

    suspend fun getTskReferencesForVerse(bookId: Int, chapter: Int, verse: Int, limit: Int = 15): List<CrossReference> = withContext(Dispatchers.IO) { dbHelper.getTskReferencesForVerse(bookId, chapter, verse, limit) }

    suspend fun parseAndResolveOfficialNote(noteText: String): List<ResolvedReference> = withContext(Dispatchers.IO) { dbHelper.parseAndResolveOfficialNote(noteText) }

    suspend fun getBookName(bookId: Int): String = withContext(Dispatchers.IO) { dbHelper.getBookName(bookId) }

    suspend fun getCompendioForVerse(bookId: Int, chapter: Int, verse: Int): List<CompendioItem> = withContext(Dispatchers.IO) { dbHelper.getCompendioForVerse(bookId, chapter, verse) }

    suspend fun getCompendioVersesForChapter(bookId: Int, chapter: Int): Set<Int> = withContext(Dispatchers.IO) { dbHelper.getCompendioVersesForChapter(bookId, chapter) }

    suspend fun getCanonicalBooksWithCompendio(): List<Pair<Book, Int>> = withContext(Dispatchers.IO) { dbHelper.getCanonicalBooksWithCompendio() }

    suspend fun getCompendioForBook(bookId: Int): List<CompendioItem> = withContext(Dispatchers.IO) { dbHelper.getCompendioForBook(bookId) }

    suspend fun getThematicCompendioCount(): Int = withContext(Dispatchers.IO) { dbHelper.getThematicCompendioCount() }

    suspend fun getThematicCompendio(query: String = "", offset: Int = 0): List<CompendioItem> = withContext(Dispatchers.IO) { dbHelper.getThematicCompendio(query, offset) }

    suspend fun searchCompendioGlobal(query: String): List<CompendioItem> = withContext(Dispatchers.IO) { dbHelper.searchCompendioGlobal(query) }

    suspend fun getCompendioItemDetail(id: Int): CompendioItem? = withContext(Dispatchers.IO) { dbHelper.getCompendioItemDetail(id) }
}
