package org.idmji.biblia.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.idmji.biblia.model.Book
import org.idmji.biblia.model.CompendioItem
import org.idmji.biblia.model.Verse
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiGoldDark
import org.idmji.biblia.ui.theme.IdmjiNavyLight
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy

private fun openOfficialVideo(context: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "Enlace de video no disponible", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo abrir el reproductor de video", Toast.LENGTH_SHORT).show()
    }
}

private fun formatSectionLabel(seccion: String): String {
    return when (seccion) {
        "preguntas_y_respuestas" -> "Preguntas y Respuestas"
        "estudios_biblicos" -> "Estudio Bíblico"
        "meditaciones" -> "Meditación"
        "transmisiones_en_vivo" -> "Enseñanza en Vivo"
        "testimonios" -> "Testimonios"
        "alabanzas" -> "Alabanzas"
        "himnos" -> "Himnos"
        else -> if (seccion.isNotBlank()) seccion.replace("_", " ").replaceFirstChar { it.uppercase() } else "Enseñanza Oficial"
    }
}

// =========================================================================
// 1. DIÁLOGO CONTEXTUAL DE VERSÍCULO (ESTUDIOS IDMJI PARA ESTE VERSÍCULO)
// =========================================================================

@Composable
fun VerseCompendioDialog(
    book: Book?,
    verse: Verse?,
    items: List<CompendioItem>,
    isLoading: Boolean,
    onOpenGeneralCompendio: () -> Unit,
    onSelectDetailItem: (CompendioItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (verse == null) return
    val context = LocalContext.current
    val citation = "${book?.name ?: ""} ${verse.chapter}:${verse.verse}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IdmjiPrimaryNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = "Estudios Doctrinales IDMJI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = IdmjiGold
                                )
                                Text(
                                    text = citation,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                // Cita del versículo en tarjeta sepia suave
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IdmjiGold.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, IdmjiGold.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "\"${verse.text}\"",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "— $citation (RVR1960)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IdmjiGoldDark,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Contenido
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IdmjiGold)
                    }
                } else if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No hay un estudio específico registrado para este versículo directo.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    onOpenGeneralCompendio()
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = IdmjiPrimaryNavy)
                            ) {
                                Text("Explorar Doctrina IDMJI", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Enseñanzas oficiales de este pasaje (${items.size}):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IdmjiPrimaryNavy,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(items) { item ->
                            CompendioItemCard(
                                item = item,
                                onOpenVideo = { openOfficialVideo(context, item.videoUrl) },
                                onReadTranscription = { onSelectDetailItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. DIÁLOGO GENERAL DEL COMPENDIO DOCTRINAL IDMJI
// =========================================================================

@Composable
fun GeneralCompendioDialog(
    booksWithCounts: List<Pair<Book, Int>>,
    selectedBook: Book?,
    bookTeachings: List<CompendioItem>,
    thematicTeachings: List<CompendioItem>,
    searchQuery: String,
    searchResults: List<CompendioItem>,
    isLoading: Boolean,
    thematicPage: Int,
    thematicHasNext: Boolean,
    isSyncing: Boolean = false,
    syncStatus: String? = null,
    onSyncTurso: () -> Unit = {},
    onDismissSyncStatus: () -> Unit = {},
    onThematicPageChange: (Int) -> Unit,
    onSelectBook: (Book?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectDetailItem: (CompendioItem) -> Unit,
    onNavigateToPassage: (bookId: Int, chapter: Int, verse: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Principal IDMJI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IdmjiPrimaryNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                Text(
                                    text = "Doctrina IDMJI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = IdmjiGold
                                )
                                Text(
                                    text = "Enseñanzas y Estudios — Hna. María Luisa Piraquive",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 6.dp),
                                    strokeWidth = 2.dp,
                                    color = IdmjiGold
                                )
                            } else {
                                IconButton(
                                    onClick = onSyncTurso,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Sincronizar con Turso Cloud",
                                        tint = IdmjiGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        }
                    }
                }

                // Banner de estado de sincronización si está disponible
                if (syncStatus != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IdmjiGold.copy(alpha = 0.18f))
                            .clickable { onDismissSyncStatus() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = syncStatus,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IdmjiPrimaryNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar aviso",
                                modifier = Modifier.size(16.dp),
                                tint = IdmjiPrimaryNavy
                            )
                        }
                    }
                }

                // Buscador en tiempo real
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar en la Doctrina IDMJI...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = IdmjiPrimaryNavy) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdmjiGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                if (searchQuery.trim().length >= 2) {
                    // Resultados de búsqueda
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IdmjiGold)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No se encontraron coincidencias para \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Resultados encontrados (${searchResults.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IdmjiPrimaryNavy,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(searchResults) { item ->
                                CompendioItemCard(
                                    item = item,
                                    onOpenVideo = { openOfficialVideo(context, item.videoUrl) },
                                    onReadTranscription = { onSelectDetailItem(item) },
                                    onNavigateToPassage = if (item.tipo == "CANONICO" && item.libroId != null && item.capitulo != null) {
                                        { onNavigateToPassage(item.libroId, item.capitulo, item.versiculoInicio ?: 1) }
                                    } else null
                                )
                            }
                        }
                    }
                } else {
                    // Pestañas de navegación
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = IdmjiPrimaryNavy,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = IdmjiGold,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                onSelectBook(null)
                            },
                            text = {
                                Text(
                                    "Libros Bíblicos",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                onSelectBook(null)
                            },
                            text = {
                                Text(
                                    "Temas Doctrinales",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (selectedTab == 0) {
                        // TAB 0: LIBROS BÍBLICOS
                        if (selectedBook == null) {
                            // Selector de Libros con cantidad de enseñanzas
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Seleccione un libro bíblico para ver sus enseñanzas grabadas:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(booksWithCounts) { (book, count) ->
                                    Card(
                                        onClick = { onSelectBook(book) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, IdmjiGold.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(if (book.testament == "AT") IdmjiPrimaryNavy else IdmjiNavyLight),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = book.testament,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = book.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(IdmjiGold.copy(alpha = 0.18f))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$count enseñanzas",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = IdmjiGoldDark
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Lista de enseñanzas del libro seleccionado
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onSelectBook(null) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Libros", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${selectedBook.name} (${bookTeachings.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = IdmjiPrimaryNavy
                                    )
                                }

                                if (isLoading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = IdmjiGold)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(bookTeachings) { item ->
                                            CompendioItemCard(
                                                item = item,
                                                onOpenVideo = { openOfficialVideo(context, item.videoUrl) },
                                                onReadTranscription = { onSelectDetailItem(item) },
                                                onNavigateToPassage = if (item.libroId != null && item.capitulo != null) {
                                                    { onNavigateToPassage(item.libroId, item.capitulo, item.versiculoInicio ?: 1) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 1: TEMAS DOCTRINALES PAGINADOS
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Enseñanzas doctrinales · Página ${thematicPage + 1}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    OutlinedButton(enabled = thematicPage > 0 && !isLoading,
                                        onClick = { onThematicPageChange(thematicPage - 1) }) { Text("Anterior") }
                                    OutlinedButton(enabled = thematicHasNext && !isLoading,
                                        onClick = { onThematicPageChange(thematicPage + 1) }) { Text("Siguiente") }
                                }
                                if (isLoading) CircularProgressIndicator(color = IdmjiGold)
                            }
                            items(thematicTeachings, key = { it.id }) { item ->
                                CompendioItemCard(
                                    item = item,
                                    onOpenVideo = { openOfficialVideo(context, item.videoUrl) },
                                    onReadTranscription = { onSelectDetailItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. TARJETA DE CADA ENSEÑANZA COMPENDIO
// =========================================================================

@Composable
fun CompendioItemCard(
    item: CompendioItem,
    onOpenVideo: () -> Unit,
    onReadTranscription: () -> Unit,
    onNavigateToPassage: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.cita.isNotBlank()) {
                    Text(
                        text = item.cita,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IdmjiPrimaryNavy
                    )
                } else {
                    Text(
                        text = "Enseñanza Doctrinal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IdmjiPrimaryNavy
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IdmjiGold.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatSectionLabel(item.seccion),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IdmjiGoldDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ver video oficial
                FilledTonalButton(
                    onClick = onOpenVideo,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = IdmjiPrimaryNavy)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        modifier = Modifier.size(16.dp),
                        tint = IdmjiGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver Video",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Leer transcripción
                OutlinedButton(
                    onClick = onReadTranscription,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Leer",
                        modifier = Modifier.size(14.dp),
                        tint = IdmjiPrimaryNavy
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Leer Estudio",
                        fontSize = 12.sp,
                        color = IdmjiPrimaryNavy
                    )
                }

                // Ir al pasaje bíblico si está disponible
                if (onNavigateToPassage != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IdmjiGold.copy(alpha = 0.15f))
                            .clickable(onClick = onNavigateToPassage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir al pasaje",
                            modifier = Modifier.size(16.dp),
                            tint = IdmjiPrimaryNavy
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. DIÁLOGO LECTOR DETALLADO CON TRANSCRIPCIÓN OFICIAL Y MARCAS DE TIEMPO
// =========================================================================

@Composable
fun CompendioDetailDialog(
    item: CompendioItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fontSize by remember { mutableFloatStateOf(15f) }
    val paragraphs = remember(item.id, item.transcripcion) { TranscriptionParser.parse(item.transcripcion) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IdmjiPrimaryNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                if (item.cita.isNotBlank()) {
                                    Text(
                                        text = item.cita,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = IdmjiGold
                                    )
                                }
                                Text(
                                    text = formatSectionLabel(item.seccion),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Botones de zoom y cierre
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButtonSmall(text = "A-", onClick = { if (fontSize > 12f) fontSize -= 1.5f })
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButtonSmall(text = "A+", onClick = { if (fontSize < 24f) fontSize += 1.5f })
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        }
                    }
                }

                // Compose only the visible transcription paragraphs.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Título completo
                            Text(
                                text = item.titulo,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 23.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Barra con botón de video oficial y copiar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { openOfficialVideo(context, item.videoUrl) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = IdmjiPrimaryNavy)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Ver video", tint = IdmjiGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ver Video Oficial", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = "${item.titulo}\n${item.videoUrl}\n\nDoctrina IDMJI"
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Estudio IDMJI", clipText))
                                        Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(IdmjiGold.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar enlace", tint = IdmjiPrimaryNavy, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Transcripción Literal Oficial:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = IdmjiGoldDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                        }
                    }
                    items(paragraphs) { paragraph ->
                        TranscriptionTextView(paragraph, fontSize.sp) { url -> openOfficialVideo(context, url) }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TextButtonSmall(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TranscriptionTextView(
    paragraph: TranscriptionParagraph,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onTimestampClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        val url = paragraph.url
        if (url != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(IdmjiPrimaryNavy.copy(alpha = 0.1f))
                .clickable { onTimestampClick(url) }.padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("▶ ${paragraph.timestamp}", fontSize = (fontSize.value * 0.8f).sp,
                    fontWeight = FontWeight.Bold, color = IdmjiPrimaryNavy)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = paragraph.text, fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = (fontSize.value * 1.45f).sp, modifier = Modifier.weight(1f))
    }
}
