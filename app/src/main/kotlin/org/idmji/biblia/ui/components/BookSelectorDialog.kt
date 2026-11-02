package org.idmji.biblia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.idmji.biblia.model.Book
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiGoldDark
import org.idmji.biblia.ui.theme.IdmjiNavyLight
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy
import java.text.Normalizer

private fun String.normalizeForSearch(): String {
    return Normalizer.normalize(this.trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
}

@Composable
fun BookSelectorDialog(
    books: List<Book>,
    currentBook: Book?,
    currentChapter: Int = 1,
    onSelectChapter: (bookId: Int, chapter: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 0 = Antiguo (39), 1 = Nuevo (27), 2 = Todos (66)
    var selectedTab by remember {
        mutableIntStateOf(if (currentBook?.testament == "NT") 1 else 0)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBookForChapters by remember { mutableStateOf<Book?>(null) }

    val oldTestament = remember(books) { books.filter { it.testament == "AT" } }
    val newTestament = remember(books) { books.filter { it.testament == "NT" } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedBookForChapters == null) {
                    // ==========================================
                    // 1. CABECERA
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IdmjiPrimaryNavy)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Libros de la Sagrada Biblia",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Reina-Valera 1960",
                                color = IdmjiGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }

                    // ==========================================
                    // 2. BUSCADOR INSTANTÁNEO
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Buscar libro (ej. Génesis, Salmos, Judas...)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = IdmjiGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdmjiGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    // ==========================================
                    // 3. PESTAÑAS (ANTIGUO, NUEVO, TODOS)
                    // ==========================================
                    if (searchQuery.isBlank()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = IdmjiGold
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        "Antiguo Testamento",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 0) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        "Nuevo Testamento",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 1) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Text(
                                        "Todos",
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 2) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }

                    // ==========================================
                    // 4. LISTADO NORMAL EN 3 COLUMNAS (SIN SUBTÍTULOS)
                    // ==========================================
                    if (searchQuery.isNotBlank()) {
                        val norm = searchQuery.normalizeForSearch()
                        val filtered = books.filter { b ->
                            b.name.normalizeForSearch().contains(norm) ||
                            b.abbreviation.normalizeForSearch().contains(norm)
                        }

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron libros con \"$searchQuery\"",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered, key = { it.id }) { book ->
                                    BookGridCard(
                                        book = book,
                                        isCurrent = currentBook?.id == book.id,
                                        onClick = { selectedBookForChapters = book }
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    } else {
                        when (selectedTab) {
                            0 -> {
                                // Antiguo Testamento completo en orden canónico normal RVR 1960
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(oldTestament, key = { it.id }) { book ->
                                        BookGridCard(
                                            book = book,
                                            isCurrent = currentBook?.id == book.id,
                                            onClick = { selectedBookForChapters = book }
                                        )
                                    }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                            1 -> {
                                // Nuevo Testamento en el orden del Manual para Leer la Biblia de la IDMJI
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(newTestament, key = { it.id }) { book ->
                                        BookGridCard(
                                            book = book,
                                            isCurrent = currentBook?.id == book.id,
                                            onClick = { selectedBookForChapters = book }
                                        )
                                    }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                            else -> {
                                // Todos los 66 libros continuos
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Encabezado simple AT
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(IdmjiPrimaryNavy.copy(alpha = 0.08f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Antiguo Testamento",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = IdmjiPrimaryNavy
                                            )
                                        }
                                    }

                                    items(oldTestament, key = { it.id }) { book ->
                                        BookGridCard(
                                            book = book,
                                            isCurrent = currentBook?.id == book.id,
                                            onClick = { selectedBookForChapters = book }
                                        )
                                    }

                                    // Encabezado simple NT
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, bottom = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(IdmjiPrimaryNavy.copy(alpha = 0.08f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Nuevo Testamento",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = IdmjiPrimaryNavy
                                            )
                                        }
                                    }

                                    items(newTestament, key = { it.id }) { book ->
                                        BookGridCard(
                                            book = book,
                                            isCurrent = currentBook?.id == book.id,
                                            onClick = { selectedBookForChapters = book }
                                        )
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // ==========================================
                    // 5. VISTA DE CAPÍTULOS DEL LIBRO
                    // ==========================================
                    val book = selectedBookForChapters!!

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IdmjiPrimaryNavy)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedBookForChapters = null }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a libros",
                                tint = IdmjiGold
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        ) {
                            Text(
                                text = book.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${book.chaptersCount} capítulos • ${if (book.testament == "AT") "Antiguo Testamento" else "Nuevo Testamento"}",
                                color = IdmjiGold,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Selecciona un capítulo:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 54.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(book.chaptersCount) { index ->
                            val chapterNum = index + 1
                            val isCurrent = currentBook?.id == book.id && chapterNum == currentChapter
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) IdmjiGold
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 0.5.dp,
                                        color = if (isCurrent) IdmjiGoldDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectChapter(book.id, chapterNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chapterNum.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isCurrent) IdmjiPrimaryNavy else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookGridCard(
    book: Book,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) IdmjiGold.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isCurrent) BorderStroke(1.5.dp, IdmjiGold) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.name,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isCurrent) IdmjiGoldDark else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
