package org.idmji.biblia.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.idmji.biblia.model.Book
import org.idmji.biblia.model.CrossReference
import org.idmji.biblia.model.Pericopa
import org.idmji.biblia.model.ResolvedReference
import org.idmji.biblia.model.Verse
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiGoldDark
import org.idmji.biblia.ui.theme.IdmjiNavyLight
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy

@Composable
fun CrossReferencesDialog(
    book: Book?,
    verse: Verse?,
    officialNote: String?,
    resolvedOfficialRefs: List<ResolvedReference>,
    tskReferences: List<CrossReference>,
    pericopa: Pericopa?,
    isLoading: Boolean,
    onNavigateToPassage: (bookId: Int, chapter: Int, verse: Int) -> Unit,
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
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ==========================================
                // CABECERA PRINCIPAL
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IdmjiPrimaryNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IdmjiGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = IdmjiGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Referencias Cruzadas",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "$citation • Reina-Valera 1960",
                                color = IdmjiGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
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
                // CONTENIDO
                // ==========================================
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IdmjiGold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Versículo seleccionado
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = IdmjiPrimaryNavy.copy(alpha = 0.06f)
                                ),
                                border = BorderStroke(0.5.dp, IdmjiGold.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = citation,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = IdmjiPrimaryNavy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${verse.text}\"",
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Pasajes paralelos del encabezado
                        if (pericopa != null && !pericopa.parallelPassages.isNullOrBlank()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = IdmjiGold.copy(alpha = 0.12f)
                                    ),
                                    border = BorderStroke(1.dp, IdmjiGold.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Pasajes Paralelos de la Sección",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = IdmjiGoldDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${pericopa.title} ${pericopa.parallelPassages}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // SECCIÓN 1: Referencias Oficiales RVR1960 (Sociedades Bíblicas Unidas)
                        if (!officialNote.isNullOrBlank() || resolvedOfficialRefs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                                    Text(
                                        text = "Referencias Reina-Valera 1960 (Oficial SBU)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IdmjiPrimaryNavy
                                    )
                                    if (!officialNote.isNullOrBlank()) {
                                        Text(
                                            text = "Nota literal: $officialNote",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (resolvedOfficialRefs.isNotEmpty()) {
                                items(resolvedOfficialRefs) { ref ->
                                    ReferenceItemCard(
                                        citation = ref.citation,
                                        text = ref.text,
                                        badgeLabel = "RVR1960",
                                        badgeColor = IdmjiGold,
                                        onNavigate = {
                                            onNavigateToPassage(ref.bookId, ref.chapter, ref.verseStart)
                                        }
                                    )
                                }
                            }
                        }

                        // SECCIÓN 2: Tesoro del Conocimiento Bíblico (TSK)
                        if (tskReferences.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                                    Text(
                                        text = "Conexiones Bíblicas (Tesoro de las Escrituras)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IdmjiPrimaryNavy
                                    )
                                    Text(
                                        text = "Referencias cruzadas temáticas y proféticas",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(tskReferences) { tsk ->
                                val cit = if (tsk.destVerseStart == tsk.destVerseEnd) {
                                    "${tsk.destBookName} ${tsk.destChapter}:${tsk.destVerseStart}"
                                } else {
                                    "${tsk.destBookName} ${tsk.destChapter}:${tsk.destVerseStart}-${tsk.destVerseEnd}"
                                }
                                ReferenceItemCard(
                                    citation = cit,
                                    text = tsk.text,
                                    badgeLabel = "${tsk.votes} votos",
                                    badgeColor = IdmjiNavyLight,
                                    onNavigate = {
                                        onNavigateToPassage(tsk.destBookId, tsk.destChapter, tsk.destVerseStart)
                                    }
                                )
                            }
                        }

                        if (officialNote.isNullOrBlank() && tskReferences.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay referencias cruzadas adicionales para este versículo.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // ==========================================
                    // BARRA INFERIOR DE ACCIONES
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text("Cerrar", fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val sb = StringBuilder()
                                sb.append("Referencias cruzadas para $citation:\n\n")
                                if (!officialNote.isNullOrBlank()) {
                                    sb.append("• Nota RVR1960: $officialNote\n\n")
                                }
                                for (r in resolvedOfficialRefs) {
                                    sb.append("• ${r.citation}: \"${r.text}\"\n")
                                }
                                for (t in tskReferences.take(5)) {
                                    val cit = "${t.destBookName} ${t.destChapter}:${t.destVerseStart}"
                                    sb.append("• $cit: \"${t.text}\"\n")
                                }
                                clipboard.setPrimaryClip(ClipData.newPlainText("Referencias Cruzadas", sb.toString()))
                                Toast.makeText(context, "Referencias copiadas al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IdmjiPrimaryNavy,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdmjiGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Citas", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceItemCard(
    citation: String,
    text: String,
    badgeLabel: String,
    badgeColor: Color,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = citation,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = IdmjiPrimaryNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IdmjiPrimaryNavy
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onNavigate,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IdmjiGold.copy(alpha = 0.18f),
                        contentColor = IdmjiPrimaryNavy
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Ir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
