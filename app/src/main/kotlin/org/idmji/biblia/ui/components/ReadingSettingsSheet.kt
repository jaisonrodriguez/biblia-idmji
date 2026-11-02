package org.idmji.biblia.ui.components

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.idmji.biblia.model.ReadingSettings
import org.idmji.biblia.model.ReadingTheme
import org.idmji.biblia.ui.theme.DayBackground
import org.idmji.biblia.ui.theme.DayText
import org.idmji.biblia.ui.theme.IdmjiGold
import org.idmji.biblia.ui.theme.IdmjiPrimaryNavy
import org.idmji.biblia.ui.theme.NightBackground
import org.idmji.biblia.ui.theme.NightText
import org.idmji.biblia.ui.theme.SepiaBackground
import org.idmji.biblia.ui.theme.SepiaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateTheme: (ReadingTheme) -> Unit,
    onToggleSerif: (Boolean) -> Unit,
    onToggleVerseByVerse: (Boolean) -> Unit,
    isSyncingTurso: Boolean = false,
    isCheckingUpdate: Boolean = false,
    onSyncTurso: () -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Ajustes de Lectura",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = IdmjiPrimaryNavy
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Tamaño de Letra
            Text(
                text = "Tamaño de texto: ${settings.fontSizeSp.toInt()} sp",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = onUpdateFontSize,
                    valueRange = 14f..28f,
                    steps = 7,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = IdmjiGold,
                        activeTrackColor = IdmjiGold
                    )
                )
                Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Modos de Tema
            Text(
                text = "Modo de Fondo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOptionCard(
                    title = "Día",
                    bg = DayBackground,
                    fg = DayText,
                    isSelected = settings.theme == ReadingTheme.DAY,
                    onClick = { onUpdateTheme(ReadingTheme.DAY) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionCard(
                    title = "Sepia",
                    bg = SepiaBackground,
                    fg = SepiaText,
                    isSelected = settings.theme == ReadingTheme.SEPIA,
                    onClick = { onUpdateTheme(ReadingTheme.SEPIA) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionCard(
                    title = "Noche",
                    bg = NightBackground,
                    fg = NightText,
                    isSelected = settings.theme == ReadingTheme.NIGHT,
                    onClick = { onUpdateTheme(ReadingTheme.NIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Tipografía (Serif vs Sans)
            Text(
                text = "Tipografía",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = settings.isSerif,
                    onClick = { onToggleSerif(true) },
                    label = { Text("Serifa (Estilo Libro)", fontFamily = FontFamily.Serif) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !settings.isSerif,
                    onClick = { onToggleSerif(false) },
                    label = { Text("Sans-serif (Limpia)", fontFamily = FontFamily.SansSerif) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Formato de visualización
            Text(
                text = "Formato de Lectura",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = settings.isVerseByVerse,
                    onClick = { onToggleVerseByVerse(true) },
                    label = { Text("Versículo por línea") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !settings.isVerseByVerse,
                    onClick = { onToggleVerseByVerse(false) },
                    label = { Text("Párrafo continuo") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Actualizaciones y Sincronización",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = IdmjiPrimaryNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Sincronizar Doctrina IDMJI con Turso
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Doctrina IDMJI (Videos)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Descarga nuevas enseñanzas oficiales desde Turso Cloud",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onSyncTurso,
                        enabled = !isSyncingTurso,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = IdmjiPrimaryNavy,
                            contentColor = IdmjiGold
                        )
                    ) {
                        if (isSyncingTurso) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = IdmjiGold
                            )
                        } else {
                            Text("Sincronizar", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Buscar Actualizaciones de la App (GitHub)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Versión de la App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Buscar nueva versión en GitHub Releases",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onCheckUpdate,
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = IdmjiPrimaryNavy,
                            contentColor = IdmjiGold
                        )
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = IdmjiGold
                            )
                        } else {
                            Text("Buscar", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    bg: Color,
    fg: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) IdmjiGold else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = fg,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
