package org.idmji.biblia.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.idmji.biblia.model.Book
import org.idmji.biblia.model.Verse
import org.idmji.biblia.ui.theme.HighlightBlue
import org.idmji.biblia.ui.theme.HighlightGreen
import org.idmji.biblia.ui.theme.HighlightPink
import org.idmji.biblia.ui.theme.HighlightYellow
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy

@Composable
fun VerseActionsBar(
    book: Book?,
    verse: Verse,
    onHighlight: (String?) -> Unit,
    onOpenNote: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenCrossReferences: () -> Unit,
    onOpenCompendio: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val citation = "${book?.name ?: ""} ${verse.chapter}:${verse.verse}"
    val fullText = "\"${verse.text}\" — $citation (RVR1960)"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = citation,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = IdmjiPrimaryNavy
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Paleta de colores de resaltado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Resaltar:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HighlightColorButton(color = HighlightYellow, onClick = { onHighlight("YELLOW") })
                HighlightColorButton(color = HighlightGreen, onClick = { onHighlight("GREEN") })
                HighlightColorButton(color = HighlightBlue, onClick = { onHighlight("BLUE") })
                HighlightColorButton(color = HighlightPink, onClick = { onHighlight("PINK") })
                
                // Botón quitar color
                TextButton(onClick = { onHighlight(null) }) {
                    Text("Quitar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila 1 de Acciones: Copiar, Compartir, Nota
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Versículo Bíblico", fullText))
                    Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(15.dp), tint = IdmjiPrimaryNavy)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Copiar", fontSize = 12.sp, color = IdmjiPrimaryNavy)
                }

                TextButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, fullText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartir Versículo"))
                    onDismiss()
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(15.dp), tint = IdmjiPrimaryNavy)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Compartir", fontSize = 12.sp, color = IdmjiPrimaryNavy)
                }

                TextButton(onClick = onOpenNote) {
                    Icon(Icons.Default.Edit, contentDescription = "Nota", modifier = Modifier.size(15.dp), tint = IdmjiPrimaryNavy)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (verse.note.isNullOrEmpty()) "Nota" else "Ver Nota", fontSize = 12.sp, color = IdmjiPrimaryNavy)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Fila 2 de Acciones Especializadas: Referencias, Diccionario, Compendio IDMJI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onOpenCrossReferences) {
                    Icon(Icons.Default.Link, contentDescription = "Referencias", modifier = Modifier.size(15.dp), tint = IdmjiPrimaryNavy)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Referencias", fontSize = 12.sp, color = IdmjiPrimaryNavy)
                }

                TextButton(onClick = onOpenDictionary) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Diccionario", modifier = Modifier.size(15.dp), tint = IdmjiPrimaryNavy)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Diccionario", fontSize = 12.sp, color = IdmjiPrimaryNavy)
                }

                TextButton(onClick = onOpenCompendio) {
                    Text("Doctrina IDMJI", fontSize = 12.sp, color = IdmjiPrimaryNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HighlightColorButton(
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick)
    )
}
