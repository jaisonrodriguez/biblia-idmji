package org.idmji.biblia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.idmji.biblia.model.ReadingTheme
import org.idmji.biblia.model.Verse
import org.idmji.biblia.ui.components.BookSelectorDialog
import org.idmji.biblia.ui.components.CompendioDetailDialog
import org.idmji.biblia.ui.components.CrossReferencesDialog
import org.idmji.biblia.ui.components.DictionaryDialog
import org.idmji.biblia.ui.components.GeneralCompendioDialog
import org.idmji.biblia.ui.components.NoteDialog
import org.idmji.biblia.ui.components.ReadingSettingsSheet
import org.idmji.biblia.ui.components.SearchDialog
import org.idmji.biblia.ui.components.UpdateDialog
import org.idmji.biblia.ui.components.VerseActionsBar
import org.idmji.biblia.ui.components.VerseCompendioDialog
import org.idmji.biblia.ui.theme.BibliaIDMJITheme
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiGoldDark
import org.idmji.biblia.ui.theme.IdmjiNavyLight
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy
import org.idmji.biblia.ui.theme.parseHighlightColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(viewModel: BibleViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(state.updateCheckMessage) {
        state.updateCheckMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissUpdateCheckMessage()
        }
    }
    LaunchedEffect(state.syncCompendioStatus) {
        state.syncCompendioStatus?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissSyncStatus()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.checkForAppUpdates(manual = false)
    }

    BibliaIDMJITheme(readingTheme = state.settings.theme) {
        val fontFamily = if (state.settings.isSerif) FontFamily.Serif else FontFamily.SansSerif
        val fontSp = state.settings.fontSizeSp.sp
        val lineSpacing = (state.settings.fontSizeSp * 1.55f).sp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(IdmjiNavyLight.copy(alpha = 0.5f))
                                .clickable { viewModel.setShowBookSelector(true) }
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${state.currentBook?.name ?: ""} ${state.currentChapter}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = IdmjiGold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Cambiar libro",
                                        tint = IdmjiGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Reina-Valera 1960",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.openGeneralCompendio() }) {
                            Text("🕊️", fontSize = 18.sp)
                        }
                        IconButton(onClick = { viewModel.openDictionary() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Diccionario Bíblico",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.setShowSearchDialog(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = { viewModel.setShowSettingsSheet(true) }) {
                            Text(
                                text = "aA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = IdmjiPrimaryNavy
                    )
                )
            },
            bottomBar = {
                // Barra de navegación de capítulos inferior
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.previousChapter() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Capítulo anterior", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Anterior", fontSize = 13.sp)
                        }

                        Text(
                            text = "RVR 1960",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IdmjiGold
                        )

                        FilledTonalButton(
                            onClick = { viewModel.nextChapter() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("Siguiente", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Capítulo siguiente", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IdmjiGold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(if (state.settings.isVerseByVerse) 12.dp else 6.dp)
                    ) {
                        // Título del capítulo
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.currentBook?.name ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    fontFamily = fontFamily,
                                    color = IdmjiGold
                                )
                                Text(
                                    text = "Capítulo ${state.currentChapter}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(2.dp)
                                        .background(IdmjiGold.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // Lista de versículos
                        items(state.verses, key = { it.id }) { verse ->
                            val pericopa = state.chapterPericopas[verse.verse]
                            if (pericopa != null) {
                                PericopeHeaderView(
                                    title = pericopa.title,
                                    parallelPassages = pericopa.parallelPassages,
                                    fontFamily = fontFamily
                                )
                            }
                            VerseItemView(
                                verse = verse,
                                isSelected = state.selectedVerse?.id == verse.id,
                                isVerseByVerse = state.settings.isVerseByVerse,
                                fontFamily = fontFamily,
                                fontSize = fontSp,
                                lineHeight = lineSpacing,
                                hasCrossReferences = state.chapterOfficialNotes.containsKey(verse.verse),
                                onOpenCrossReferences = { viewModel.openCrossReferencesForVerse(verse) },
                                hasCompendio = state.chapterCompendioVerses.contains(verse.verse),
                                onOpenCompendio = { viewModel.openCompendioForVerse(verse) },
                                onClick = { viewModel.toggleVerseSelection(verse) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }

                    // Barra flotante de acciones sobre el versículo seleccionado
                    if (state.selectedVerse != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            VerseActionsBar(
                                book = state.currentBook,
                                verse = state.selectedVerse!!,
                                onHighlight = { color -> viewModel.setHighlight(color) },
                                onOpenNote = { viewModel.setShowNoteDialog(true) },
                                onOpenDictionary = { viewModel.openDictionaryForVerse(state.selectedVerse!!) },
                                onOpenCrossReferences = { viewModel.openCrossReferencesForVerse(state.selectedVerse!!) },
                                onOpenCompendio = { viewModel.openCompendioForVerse(state.selectedVerse!!) },
                                onDismiss = { viewModel.clearSelection() }
                            )
                        }
                    }
                }
            }

            // Diálogos modales
            if (state.showBookSelector) {
                BookSelectorDialog(
                    books = state.books,
                    currentBook = state.currentBook,
                    currentChapter = state.currentChapter,
                    onSelectChapter = { bookId, chapter ->
                        viewModel.selectChapter(bookId, chapter)
                    },
                    onDismiss = { viewModel.setShowBookSelector(false) }
                )
            }

            if (state.showSettingsSheet) {
                ReadingSettingsSheet(
                    settings = state.settings,
                    onUpdateFontSize = { viewModel.updateFontSize(it) },
                    onUpdateTheme = { viewModel.updateTheme(it) },
                    onToggleSerif = { viewModel.toggleFontFamily(it) },
                    onToggleVerseByVerse = { viewModel.toggleViewMode(it) },
                    isSyncingTurso = state.isSyncingCompendio,
                    isCheckingUpdate = state.isCheckingAppUpdate,
                    onSyncTurso = { viewModel.syncDoctrina() },
                    onCheckUpdate = { viewModel.checkForAppUpdates(manual = true) },
                    onDismiss = { viewModel.setShowSettingsSheet(false) }
                )
            }

            if (state.showNoteDialog && state.selectedVerse != null) {
                NoteDialog(
                    book = state.currentBook,
                    verse = state.selectedVerse!!,
                    onSaveNote = { viewModel.saveNote(it) },
                    onDismiss = { viewModel.setShowNoteDialog(false) }
                )
            }

            if (state.showSearchDialog) {
                SearchDialog(
                    query = state.searchQuery,
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    onQueryChange = { viewModel.search(it) },
                    onSelectResult = { bookId, chapter ->
                        viewModel.selectChapter(bookId, chapter)
                        viewModel.setShowSearchDialog(false)
                    },
                    onDismiss = { viewModel.setShowSearchDialog(false) }
                )
            }

            if (state.showDictionaryDialog) {
                DictionaryDialog(
                    letters = state.dictionaryLetters,
                    selectedLetter = state.selectedDictionaryLetter,
                    searchQuery = state.dictionarySearchQuery,
                    entries = state.dictionaryEntries,
                    selectedEntry = state.selectedDictionaryEntry,
                    isLoading = state.isDictionaryLoading,
                    verseCitation = state.dictionaryVerseCitation,
                    onSelectLetter = { viewModel.selectDictionaryLetter(it) },
                    onSearchQueryChange = { viewModel.searchDictionary(it) },
                    onSelectEntry = { viewModel.selectDictionaryEntry(it) },
                    onDismiss = { viewModel.closeDictionary() }
                )
            }

            if (state.showCrossReferencesDialog) {
                CrossReferencesDialog(
                    book = state.currentBook,
                    verse = state.crossReferenceVerse,
                    officialNote = state.currentOfficialNote,
                    resolvedOfficialRefs = state.currentResolvedOfficialRefs,
                    tskReferences = state.currentTskReferences,
                    pericopa = state.currentPericopaForVerse,
                    isLoading = state.isLoadingCrossReferences,
                    onNavigateToPassage = { bookId, chapter, verse ->
                        viewModel.navigateToPassage(bookId, chapter, verse)
                    },
                    onDismiss = { viewModel.closeCrossReferences() }
                )
            }

            if (state.showVerseCompendioDialog) {
                VerseCompendioDialog(
                    book = state.currentBook,
                    verse = state.verseCompendioTargetVerse,
                    items = state.verseCompendioItems,
                    isLoading = state.isCompendioLoading,
                    onOpenGeneralCompendio = { viewModel.openGeneralCompendio() },
                    onSelectDetailItem = { viewModel.selectCompendioItem(it) },
                    onDismiss = { viewModel.closeVerseCompendio() }
                )
            }

            if (state.showGeneralCompendioDialog) {
                GeneralCompendioDialog(
                    booksWithCounts = state.compendioBooksWithCounts,
                    selectedBook = state.compendioSelectedBook,
                    bookTeachings = state.compendioBookTeachings,
                    thematicTeachings = state.compendioThematicTeachings,
                    searchQuery = state.compendioSearchQuery,
                    searchResults = state.compendioSearchResults,
                    isLoading = state.isCompendioLoading,
                    thematicPage = state.compendioThematicPage,
                    thematicHasNext = state.compendioThematicHasNext,
                    isSyncing = state.isSyncingCompendio,
                    syncStatus = state.syncCompendioStatus,
                    onSyncTurso = { viewModel.syncDoctrina() },
                    onDismissSyncStatus = { viewModel.dismissSyncStatus() },
                    onThematicPageChange = viewModel::loadThematicPage,
                    onSelectBook = { viewModel.selectCompendioBook(it) },
                    onSearchQueryChange = { viewModel.searchCompendio(it) },
                    onSelectDetailItem = { viewModel.selectCompendioItem(it) },
                    onNavigateToPassage = { bookId, chapter, verse ->
                        viewModel.navigateToCompendioPassage(bookId, chapter, verse)
                    },
                    onDismiss = { viewModel.closeGeneralCompendio() }
                )
            }

            if (state.showUpdateDialog && state.appUpdateInfo != null) {
                UpdateDialog(
                    updateInfo = state.appUpdateInfo!!,
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }

            if (state.isCompendioDetailLoading) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.selectCompendioItem(null) }) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    }
                }
            }
            if (state.selectedCompendioItem != null) {
                CompendioDetailDialog(
                    item = state.selectedCompendioItem!!,
                    onDismiss = { viewModel.selectCompendioItem(null) }
                )
            }
        }
    }
}

@Composable
fun VerseItemView(
    verse: Verse,
    isSelected: Boolean,
    isVerseByVerse: Boolean,
    fontFamily: FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    hasCrossReferences: Boolean = false,
    onOpenCrossReferences: () -> Unit = {},
    hasCompendio: Boolean = false,
    onOpenCompendio: () -> Unit = {},
    onClick: () -> Unit
) {
    val highlightColor = parseHighlightColor(verse.highlightColor)
    val hasNote = !verse.note.isNullOrEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> IdmjiGold.copy(alpha = 0.25f)
                    highlightColor != null -> highlightColor.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        val annotatedText = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = IdmjiGold,
                    fontSize = (fontSize.value * 0.75f).sp
                )
            ) {
                append("${verse.verse} ")
            }
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = fontFamily,
                    fontSize = fontSize
                )
            ) {
                append(verse.text)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = annotatedText,
                modifier = Modifier.weight(1f),
                lineHeight = lineHeight
            )

            if (hasCompendio) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(IdmjiPrimaryNavy.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenCompendio),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕊️",
                        fontSize = 11.sp
                    )
                }
            }

            if (hasCrossReferences) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(IdmjiGold.copy(alpha = 0.15f))
                        .clickable(onClick = onOpenCrossReferences),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = "Referencias cruzadas",
                        modifier = Modifier.size(14.dp),
                        tint = IdmjiPrimaryNavy
                    )
                }
            }

            if (hasNote) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Nota adjunta",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 4.dp),
                    tint = IdmjiGold
                )
            }
        }
    }
}

@Composable
fun PericopeHeaderView(
    title: String,
    parallelPassages: String?,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp, start = 6.dp, end = 6.dp)
    ) {
        Text(
            text = title,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = IdmjiPrimaryNavy,
            lineHeight = 22.sp
        )
        if (!parallelPassages.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = parallelPassages,
                fontFamily = fontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = IdmjiGoldDark,
                lineHeight = 18.sp
            )
        }
    }
}
