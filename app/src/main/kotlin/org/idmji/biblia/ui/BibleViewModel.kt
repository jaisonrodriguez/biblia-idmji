package org.idmji.biblia.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
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
import org.idmji.biblia.sync.AppUpdateInfo
import org.idmji.biblia.sync.SyncResult
import org.idmji.biblia.sync.UpdateCheckResult

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
    val errorMessage: String? = null,
    val compendioThematicTotal: Int = 0,
    val compendioThematicPage: Int = 0,
    val compendioThematicHasNext: Boolean = false,
    val isCompendioDetailLoading: Boolean = false,
    val isCompendioLoading: Boolean = false,
    // Sincronización y Actualizaciones
    val isSyncingCompendio: Boolean = false,
    val syncCompendioStatus: String? = null,
    val isCheckingAppUpdate: Boolean = false,
    val appUpdateInfo: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val updateCheckMessage: String? = null
)

class BibleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BibleRepository(application)

    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    private var chapterJob: Job? = null
    private var compendioJob: Job? = null
    private var compendioSearchJob: Job? = null
    private var compendioDetailJob: Job? = null

    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }

    private fun reportError(error: Exception) {
        android.util.Log.e("BibleViewModel", "Error al cargar datos locales", error)
        _uiState.update { it.copy(errorMessage = "No se pudieron cargar los datos. Intenta de nuevo.") }
    }

    private fun loadCompendio(block: suspend () -> Unit) {
        compendioJob?.cancel()
        _uiState.update { it.copy(isCompendioLoading = true, errorMessage = null) }
        compendioJob = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { reportError(error) }
            finally {
                if (currentCoroutineContext().isActive) {
                    _uiState.update { it.copy(isCompendioLoading = false) }
                }
            }
        }
    }


    init {
        viewModelScope.launch {
            // Cargar configuración
            launch {
                repository.settings.collect { newSettings ->
                    _uiState.update { it.copy(settings = newSettings) }
                }
            }

            try {
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportError(error)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectChapter(bookId: Int, chapter: Int) {
        chapterJob?.cancel()
        chapterJob = viewModelScope.launch {
            try {
                val book = _uiState.value.books.find { it.id == bookId } ?: return@launch
                val validChapter = chapter.coerceIn(1, book.chaptersCount)
                val versesList = repository.getVerses(book.id, validChapter)
                val pericopasMap = repository.getPericopasForChapter(book.id, validChapter)
                val notesMap = repository.getOfficialNotesForChapter(book.id, validChapter)
                val compendioVerses = repository.getCompendioVersesForChapter(book.id, validChapter)

                currentCoroutineContext().ensureActive()
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) { reportError(error) }
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
        _uiState.update { it.copy(showVerseCompendioDialog = true,
            verseCompendioTargetVerse = verse, verseCompendioItems = emptyList()) }
        loadCompendio {
            val items = repository.getCompendioForVerse(book.id, verse.chapter, verse.verse)
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(verseCompendioItems = items) }
        }
    }

    fun closeVerseCompendio() {
        compendioJob?.cancel()
        selectCompendioItem(null)
        _uiState.update { it.copy(showVerseCompendioDialog = false,
            verseCompendioTargetVerse = null, verseCompendioItems = emptyList(), isCompendioLoading = false) }
    }

    fun openGeneralCompendio() {
        closeVerseCompendio()
        compendioSearchJob?.cancel()
        _uiState.update { it.copy(showGeneralCompendioDialog = true,
            compendioSelectedBook = null, compendioBookTeachings = emptyList(),
            compendioSearchQuery = "", compendioSearchResults = emptyList(),
            compendioThematicTeachings = emptyList(), compendioThematicPage = 0,
            compendioThematicHasNext = false) }
        loadCompendio {
            val books = repository.getCanonicalBooksWithCompendio()
            val total = repository.getThematicCompendioCount()
            val page = repository.getThematicCompendio()
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(compendioBooksWithCounts = books,
                compendioThematicTeachings = page, compendioThematicTotal = total,
                compendioThematicHasNext = page.size < total) }
        }
    }

    fun closeGeneralCompendio() {
        compendioJob?.cancel()
        compendioSearchJob?.cancel()
        selectCompendioItem(null)
        _uiState.update { it.copy(showGeneralCompendioDialog = false,
            isCompendioLoading = false, compendioSelectedBook = null,
            compendioBookTeachings = emptyList(), compendioThematicTeachings = emptyList(),
            compendioSearchQuery = "", compendioSearchResults = emptyList()) }
    }

    fun selectCompendioBook(book: Book?) {
        compendioJob?.cancel()
        _uiState.update { it.copy(compendioSelectedBook = book,
            compendioBookTeachings = emptyList(), isCompendioLoading = false) }
        if (book == null) return
        loadCompendio {
            val teachings = repository.getCompendioForBook(book.id)
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(compendioBookTeachings = teachings) }
        }
    }

    fun loadThematicPage(page: Int) {
        if (page < 0 || _uiState.value.isCompendioLoading) return
        loadCompendio {
            val teachings = repository.getThematicCompendio(offset = page * 200)
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(compendioThematicPage = page,
                compendioThematicTeachings = teachings, compendioThematicHasNext = page * 200 + teachings.size < it.compendioThematicTotal) }
        }
    }

    fun searchCompendio(query: String) {
        compendioSearchJob?.cancel()
        _uiState.update { it.copy(compendioSearchQuery = query, compendioSearchResults = emptyList()) }
        if (query.trim().length < 2) return
        compendioSearchJob = viewModelScope.launch {
            try {
                delay(300)
                val results = repository.searchCompendioGlobal(query.trim())
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(compendioSearchResults = results) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { reportError(error) }
        }
    }

    fun selectCompendioItem(item: CompendioItem?) {
        compendioDetailJob?.cancel()
        _uiState.update { it.copy(selectedCompendioItem = null, isCompendioDetailLoading = item != null) }
        if (item == null) return
        compendioDetailJob = viewModelScope.launch {
            try {
                val detail = repository.getCompendioItemDetail(item.id)
                    ?: error("La enseñanza ya no está disponible")
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(selectedCompendioItem = detail) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { reportError(error) }
            finally {
                if (currentCoroutineContext().isActive) {
                    _uiState.update { it.copy(isCompendioDetailLoading = false) }
                }
            }
        }
    }

    fun dismissSyncStatus() {
        _uiState.update { it.copy(syncCompendioStatus = null) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissUpdateCheckMessage() {
        _uiState.update { it.copy(updateCheckMessage = null) }
    }

    fun syncDoctrina() {
        if (_uiState.value.isSyncingCompendio) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCompendio = true, syncCompendioStatus = "Conectando con Turso Cloud...") }
            try {
                when (val result = repository.syncCompendioWithTurso()) {
                    is SyncResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isSyncingCompendio = false,
                                syncCompendioStatus = "¡Doctrina actualizada! Se agregaron ${result.newItemsCount} nuevas enseñanzas (Total: ${result.totalCount})."
                            )
                        }
                        if (_uiState.value.showGeneralCompendioDialog) {
                            val books = repository.getCanonicalBooksWithCompendio()
                            val total = repository.getThematicCompendioCount()
                            val page = repository.getThematicCompendio()
                            _uiState.update {
                                it.copy(
                                    compendioBooksWithCounts = books,
                                    compendioThematicTeachings = page,
                                    compendioThematicTotal = total,
                                    compendioThematicHasNext = page.size < total
                                )
                            }
                        }
                    }
                    is SyncResult.AlreadyUpToDate -> {
                        _uiState.update {
                            it.copy(
                                isSyncingCompendio = false,
                                syncCompendioStatus = "La Doctrina IDMJI ya está al día (${result.totalCount} enseñanzas disponibles)."
                            )
                        }
                    }
                    is SyncResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSyncingCompendio = false,
                                syncCompendioStatus = "Aviso de sincronización: ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncingCompendio = false,
                        syncCompendioStatus = "No se pudo sincronizar: ${e.localizedMessage ?: "Verifica tu conexión"}"
                    )
                }
            }
        }
    }

    fun checkForAppUpdates(manual: Boolean = true) {
        if (_uiState.value.isCheckingAppUpdate) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAppUpdate = true, updateCheckMessage = if (manual) "Comprobando actualizaciones en GitHub..." else null) }
            try {
                val currentVersion = try {
                    val packageInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                    packageInfo.versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }

                when (val result = repository.checkForAppUpdates(currentVersion)) {
                    is UpdateCheckResult.Available -> {
                        _uiState.update {
                            it.copy(
                                isCheckingAppUpdate = false,
                                appUpdateInfo = result.info,
                                showUpdateDialog = true,
                                updateCheckMessage = null
                            )
                        }
                    }
                    is UpdateCheckResult.UpToDate -> {
                        _uiState.update {
                            it.copy(
                                isCheckingAppUpdate = false,
                                updateCheckMessage = if (manual) "Tienes la versión más reciente (v${result.currentVersion})." else null
                            )
                        }
                    }
                    is UpdateCheckResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isCheckingAppUpdate = false,
                                updateCheckMessage = if (manual) "No se pudo comprobar: ${result.message}" else null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingAppUpdate = false,
                        updateCheckMessage = if (manual) "Error al conectar: ${e.localizedMessage ?: "Verifica tu conexión"}" else null
                    )
                }
            }
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
