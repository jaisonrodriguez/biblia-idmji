package org.idmji.biblia.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.idmji.biblia.data.BibleRepository
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

data class BibleUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val currentBook: Book? = null,
    val currentChapter: Int = 1,
    val verses: List<Verse> = emptyList(),
    val chapterPericopas: Map<Int, Pericopa> = emptyMap(),
    val chapterOfficialNotes: Map<Int, String> = emptyMap(),
    val selectedVerse: Verse? = null,
    val settings: ReadingSettings = ReadingSettings(),
    val showBookSelector: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showNoteDialog: Boolean = false,
    val showSearchDialog: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val showDictionaryDialog: Boolean = false,
    val dictionaryLetters: List<DictionaryLetter> = emptyList(),
    val selectedDictionaryLetter: String? = "A",
    val dictionarySearchQuery: String = "",
    val dictionaryEntries: List<DictionaryEntry> = emptyList(),
    val selectedDictionaryEntry: DictionaryEntry? = null,
    val isDictionaryLoading: Boolean = false,
    val dictionaryVerseCitation: String? = null,
    val dictionaryVerseMatches: List<DictionaryEntry> = emptyList(),
    // Referencias cruzadas
    val showCrossReferencesDialog: Boolean = false,
    val crossReferenceVerse: Verse? = null,
    val currentOfficialNote: String? = null,
    val currentResolvedOfficialRefs: List<ResolvedReference> = emptyList(),
    val currentTskReferences: List<CrossReference> = emptyList(),
    val currentPericopaForVerse: Pericopa? = null,
    val isLoadingCrossReferences: Boolean = false,
    // Compendio Doctrinal IDMJI
    val chapterCompendioVerses: Set<Int> = emptySet(),
    val showVerseCompendioDialog: Boolean = false,
    val verseCompendioItems: List<CompendioItem> = emptyList(),
    val verseCompendioTargetVerse: Verse? = null,
    val showGeneralCompendioDialog: Boolean = false,
    val compendioBooksWithCounts: List<Pair<Book, Int>> = emptyList(),
    val compendioSelectedBook: Book? = null,
    val compendioBookTeachings: List<CompendioItem> = emptyList(),
    val compendioThematicTeachings: List<CompendioItem> = emptyList(),
    val compendioSearchQuery: String = "",
    val compendioSearchResults: List<CompendioItem> = emptyList(),
    val selectedCompendioItem: CompendioItem? = null,
    val isCompendioLoading: Boolean = false
)

class BibleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BibleRepository(application)

    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Cargar configuración
            launch {
                repository.settings.collect { newSettings ->
                    _uiState.update { it.copy(settings = newSettings) }
                }
            }

            // Cargar libros y última posición
            val allBooks = repository.getBooks()
            val (lastBookId, lastChapter, _) = repository.getLastPosition()
            val initialBook = allBooks.find { it.id == lastBookId } ?: allBooks.firstOrNull() ?: Book(1, "Génesis", "Gn", "AT", "Pentateuco", 50)
            
            val initialVerses = repository.getVerses(initialBook.id, lastChapter)
            val initialPericopas = repository.getPericopasForChapter(initialBook.id, lastChapter)
            val initialNotes = repository.getOfficialNotesForChapter(initialBook.id, lastChapter)
            val initialCompendioVerses = repository.getCompendioVersesForChapter(initialBook.id, lastChapter)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    books = allBooks,
                    currentBook = initialBook,
                    currentChapter = lastChapter,
                    verses = initialVerses,
                    chapterPericopas = initialPericopas,
                    chapterOfficialNotes = initialNotes,
                    chapterCompendioVerses = initialCompendioVerses
                )
            }
        }
    }

    fun selectChapter(bookId: Int, chapter: Int) {
        viewModelScope.launch {
            val book = _uiState.value.books.find { it.id == bookId } ?: return@launch
            val validChapter = chapter.coerceIn(1, book.chaptersCount)
            val versesList = repository.getVerses(book.id, validChapter)
            val pericopasMap = repository.getPericopasForChapter(book.id, validChapter)
            val notesMap = repository.getOfficialNotesForChapter(book.id, validChapter)
            val compendioVerses = repository.getCompendioVersesForChapter(book.id, validChapter)

            _uiState.update {
                it.copy(
                    currentBook = book,
                    currentChapter = validChapter,
                    verses = versesList,
                    chapterPericopas = pericopasMap,
                    chapterOfficialNotes = notesMap,
                    chapterCompendioVerses = compendioVerses,
                    selectedVerse = null,
                    showBookSelector = false
                )
            }
            repository.saveLastPosition(book.id, validChapter, 1)
        }
    }

    fun nextChapter() {
        val current = _uiState.value.currentBook ?: return
        val currentCap = _uiState.value.currentChapter

        if (currentCap < current.chaptersCount) {
            selectChapter(current.id, currentCap + 1)
        } else {
            // Siguiente libro si no es el último (Apocalipsis)
            val currentIndex = _uiState.value.books.indexOfFirst { it.id == current.id }
            if (currentIndex in 0 until _uiState.value.books.size - 1) {
                val nextBook = _uiState.value.books[currentIndex + 1]
                selectChapter(nextBook.id, 1)
            }
        }
    }

    fun previousChapter() {
        val current = _uiState.value.currentBook ?: return
        val currentCap = _uiState.value.currentChapter

        if (currentCap > 1) {
            selectChapter(current.id, currentCap - 1)
        } else {
            // Libro anterior si no es el primero (Génesis)
            val currentIndex = _uiState.value.books.indexOfFirst { it.id == current.id }
            if (currentIndex > 0) {
                val prevBook = _uiState.value.books[currentIndex - 1]
                selectChapter(prevBook.id, prevBook.chaptersCount)
            }
        }
    }

    fun toggleVerseSelection(verse: Verse) {
        _uiState.update {
            it.copy(selectedVerse = if (it.selectedVerse?.id == verse.id) null else verse)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedVerse = null) }
    }

    fun setHighlight(color: String?) {
        val selected = _uiState.value.selectedVerse ?: return
        viewModelScope.launch {
            repository.setHighlight(selected.bookId, selected.chapter, selected.verse, color)
            val updatedVerses = repository.getVerses(selected.bookId, selected.chapter)
            _uiState.update { it.copy(verses = updatedVerses, selectedVerse = null) }
        }
    }

    fun saveNote(note: String) {
        val selected = _uiState.value.selectedVerse ?: return
        viewModelScope.launch {
            repository.saveNote(selected.bookId, selected.chapter, selected.verse, note)
            val updatedVerses = repository.getVerses(selected.bookId, selected.chapter)
            _uiState.update { it.copy(verses = updatedVerses, selectedVerse = null, showNoteDialog = false) }
        }
    }

    fun updateFontSize(newSizeSp: Float) {
        val current = _uiState.value.settings
        repository.updateSettings(current.copy(fontSizeSp = newSizeSp.coerceIn(12f, 32f)))
    }

    fun updateTheme(theme: ReadingTheme) {
        val current = _uiState.value.settings
        repository.updateSettings(current.copy(theme = theme))
    }

    fun toggleFontFamily(isSerif: Boolean) {
        val current = _uiState.value.settings
        repository.updateSettings(current.copy(isSerif = isSerif))
    }

    fun toggleViewMode(isVerseByVerse: Boolean) {
        val current = _uiState.value.settings
        repository.updateSettings(current.copy(isVerseByVerse = isVerseByVerse))
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val results = repository.searchVerses(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun setShowBookSelector(show: Boolean) {
        _uiState.update { it.copy(showBookSelector = show) }
    }

    fun setShowSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun setShowNoteDialog(show: Boolean) {
        _uiState.update { it.copy(showNoteDialog = show) }
    }

    fun setShowSearchDialog(show: Boolean) {
        _uiState.update { it.copy(showSearchDialog = show, searchQuery = "", searchResults = emptyList()) }
    }

    fun openDictionary(initialQuery: String? = null) {
        _uiState.update {
            it.copy(
                showDictionaryDialog = true,
                dictionarySearchQuery = initialQuery ?: "",
                selectedDictionaryEntry = null,
                dictionaryVerseCitation = null,
                dictionaryVerseMatches = emptyList(),
                selectedDictionaryLetter = if (initialQuery.isNullOrBlank()) (_uiState.value.selectedDictionaryLetter ?: "A") else null,
                isDictionaryLoading = true
            )
        }
        viewModelScope.launch {
            val letters = if (_uiState.value.dictionaryLetters.isEmpty()) {
                repository.getDictionaryLetters()
            } else {
                _uiState.value.dictionaryLetters
            }

            val entries = if (!initialQuery.isNullOrBlank()) {
                repository.getDictionaryEntries(query = initialQuery)
            } else {
                val letterToUse = _uiState.value.selectedDictionaryLetter ?: "A"
                repository.getDictionaryEntries(letter = letterToUse)
            }

            _uiState.update {
                it.copy(
                    dictionaryLetters = letters,
                    dictionaryEntries = entries,
                    isDictionaryLoading = false
                )
            }
        }
    }

    fun closeDictionary() {
        _uiState.update {
            it.copy(
                showDictionaryDialog = false,
                selectedDictionaryEntry = null,
                dictionaryVerseCitation = null,
                dictionaryVerseMatches = emptyList()
            )
        }
    }

    fun selectDictionaryLetter(letter: String?) {
        _uiState.update {
            it.copy(
                selectedDictionaryLetter = letter,
                dictionarySearchQuery = "",
                isDictionaryLoading = true
            )
        }
        viewModelScope.launch {
            val entries = if (letter == null) {
                if (_uiState.value.dictionaryVerseMatches.isNotEmpty()) {
                    _uiState.value.dictionaryVerseMatches
                } else {
                    repository.getDictionaryEntries(letter = "A")
                }
            } else {
                repository.getDictionaryEntries(letter = letter)
            }
            _uiState.update {
                it.copy(
                    dictionaryEntries = entries,
                    isDictionaryLoading = false
                )
            }
        }
    }

    fun searchDictionary(query: String) {
        _uiState.update {
            it.copy(
                dictionarySearchQuery = query,
                isDictionaryLoading = true
            )
        }
        viewModelScope.launch {
            val entries = if (query.trim().isEmpty()) {
                if (_uiState.value.selectedDictionaryLetter == null && _uiState.value.dictionaryVerseMatches.isNotEmpty()) {
                    _uiState.value.dictionaryVerseMatches
                } else {
                    val letter = _uiState.value.selectedDictionaryLetter ?: "A"
                    repository.getDictionaryEntries(letter = letter)
                }
            } else {
                repository.getDictionaryEntries(query = query)
            }
            _uiState.update {
                it.copy(
                    dictionaryEntries = entries,
                    isDictionaryLoading = false
                )
            }
        }
    }

    fun selectDictionaryEntry(entry: DictionaryEntry?) {
        _uiState.update { it.copy(selectedDictionaryEntry = entry) }
    }

    fun openDictionaryForVerse(verse: Verse) {
        val citation = "${_uiState.value.currentBook?.name ?: ""} ${verse.chapter}:${verse.verse}".trim()
        val rawWords = verse.text.split(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ]+")).filter { it.length >= 3 }
        _uiState.update {
            it.copy(
                showDictionaryDialog = true,
                dictionaryVerseCitation = citation,
                selectedDictionaryLetter = null,
                dictionarySearchQuery = "",
                selectedDictionaryEntry = null,
                isDictionaryLoading = true
            )
        }
        viewModelScope.launch {
            val letters = if (_uiState.value.dictionaryLetters.isEmpty()) {
                repository.getDictionaryLetters()
            } else {
                _uiState.value.dictionaryLetters
            }

            val matches = repository.getDictionaryEntriesForWords(rawWords)
            val entries = if (matches.isNotEmpty()) matches else repository.getDictionaryEntries(letter = "A")

            _uiState.update {
                it.copy(
                    dictionaryLetters = letters,
                    dictionaryEntries = entries,
                    dictionaryVerseMatches = matches,
                    isDictionaryLoading = false
                )
            }
        }
    }

    // ==========================================
    // REFERENCIAS CRUZADAS
    // ==========================================

    fun openCrossReferencesForVerse(verse: Verse) {
        val book = _uiState.value.currentBook ?: return
        _uiState.update {
            it.copy(
                showCrossReferencesDialog = true,
                crossReferenceVerse = verse,
                isLoadingCrossReferences = true,
                currentOfficialNote = null,
                currentResolvedOfficialRefs = emptyList(),
                currentTskReferences = emptyList(),
                currentPericopaForVerse = null
            )
        }
        viewModelScope.launch {
            val officialNote = repository.getOfficialNoteForVerse(book.id, verse.chapter, verse.verse)
            val resolvedRefs = if (!officialNote.isNullOrBlank()) {
                repository.parseAndResolveOfficialNote(officialNote)
            } else {
                emptyList()
            }
            val pericopa = repository.getPericopaForVerse(book.id, verse.chapter, verse.verse)
            val tskRefs = repository.getTskReferencesForVerse(book.id, verse.chapter, verse.verse, limit = 15)

            _uiState.update {
                it.copy(
                    currentOfficialNote = officialNote,
                    currentResolvedOfficialRefs = resolvedRefs,
                    currentTskReferences = tskRefs,
                    currentPericopaForVerse = pericopa,
                    isLoadingCrossReferences = false
                )
            }
        }
    }

    fun closeCrossReferences() {
        _uiState.update {
            it.copy(
                showCrossReferencesDialog = false,
                crossReferenceVerse = null,
                currentOfficialNote = null,
                currentResolvedOfficialRefs = emptyList(),
                currentTskReferences = emptyList(),
                currentPericopaForVerse = null,
                isLoadingCrossReferences = false
            )
        }
    }

    fun navigateToPassage(bookId: Int, chapter: Int, verse: Int) {
        closeCrossReferences()
        closeDictionary()
        clearSelection()
        selectChapter(bookId, chapter)
    }

    // ==========================================
    // COMPENDIO DOCTRINAL IDMJI
    // ==========================================

    fun openCompendioForVerse(verse: Verse) {
        val book = _uiState.value.currentBook ?: return
        _uiState.update {
            it.copy(
                showVerseCompendioDialog = true,
                verseCompendioTargetVerse = verse,
                isCompendioLoading = true,
                verseCompendioItems = emptyList()
            )
        }
        viewModelScope.launch {
            val items = repository.getCompendioForVerse(book.id, verse.chapter, verse.verse)
            _uiState.update {
                it.copy(
                    verseCompendioItems = items,
                    isCompendioLoading = false
                )
            }
        }
    }

    fun closeVerseCompendio() {
        _uiState.update {
            it.copy(
                showVerseCompendioDialog = false,
                verseCompendioTargetVerse = null,
                verseCompendioItems = emptyList(),
                isCompendioLoading = false
            )
        }
    }

    fun openGeneralCompendio() {
        _uiState.update {
            it.copy(
                showGeneralCompendioDialog = true,
                isCompendioLoading = true,
                compendioSelectedBook = null,
                compendioBookTeachings = emptyList(),
                compendioSearchQuery = "",
                compendioSearchResults = emptyList(),
                selectedCompendioItem = null
            )
        }
        viewModelScope.launch {
            val booksWithCounts = repository.getCanonicalBooksWithCompendio()
            val thematicList = repository.getThematicCompendio()
            _uiState.update {
                it.copy(
                    compendioBooksWithCounts = booksWithCounts,
                    compendioThematicTeachings = thematicList,
                    isCompendioLoading = false
                )
            }
        }
    }

    fun closeGeneralCompendio() {
        _uiState.update {
            it.copy(
                showGeneralCompendioDialog = false,
                compendioSelectedBook = null,
                compendioBookTeachings = emptyList(),
                selectedCompendioItem = null,
                compendioSearchQuery = "",
                compendioSearchResults = emptyList()
            )
        }
    }

    fun selectCompendioBook(book: Book?) {
        if (book == null) {
            _uiState.update {
                it.copy(
                    compendioSelectedBook = null,
                    compendioBookTeachings = emptyList()
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                compendioSelectedBook = book,
                isCompendioLoading = true
            )
        }
        viewModelScope.launch {
            val teachings = repository.getCompendioForBook(book.id)
            _uiState.update {
                it.copy(
                    compendioBookTeachings = teachings,
                    isCompendioLoading = false
                )
            }
        }
    }

    fun searchCompendio(query: String) {
        _uiState.update { it.copy(compendioSearchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(compendioSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = repository.searchCompendioGlobal(query.trim())
            _uiState.update { it.copy(compendioSearchResults = results) }
        }
    }

    fun selectCompendioItem(item: CompendioItem?) {
        if (item == null) {
            _uiState.update { it.copy(selectedCompendioItem = null) }
            return
        }
        if (item.transcripcion.isNotBlank()) {
            _uiState.update { it.copy(selectedCompendioItem = item) }
            return
        }
        viewModelScope.launch {
            val detailedItem = repository.getCompendioItemDetail(item.id) ?: item
            _uiState.update { it.copy(selectedCompendioItem = detailedItem) }
        }
    }

    fun navigateToCompendioPassage(bookId: Int, chapter: Int, verse: Int) {
        closeVerseCompendio()
        closeGeneralCompendio()
        closeCrossReferences()
        closeDictionary()
        clearSelection()
        selectChapter(bookId, chapter)
    }
}
