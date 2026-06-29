package com.tapman104.mpvplayer.player.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.player.model.SubtitleTrack

@Composable
fun SubtitleTrackDialog(
    tracks: List<SubtitleTrack>,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onDismiss: () -> Unit,
    onAppearanceClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        confirmButton = {
            Button(
                onClick = onAppearanceClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Appearance", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        },
        title = {
            Text(
                text = "Subtitle Track",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            if (tracks.isEmpty()) {
                Text(
                    text = "No subtitle tracks available",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDisableSubtitles()
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Disable Subtitles",
                                color = if (selectedTrackId == -1) Color(0xFF8B5CF6) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTrackId == -1) FontWeight.Medium else FontWeight.Normal
                            )
                            val isSelected = selectedTrackId == -1
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF8B5CF6),
                                    unselectedColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                        }
                        if (tracks.isNotEmpty()) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                    itemsIndexed(tracks) { index, track ->
                        TrackRow(
                            title = track.title,
                            lang = track.lang,
                            isSelected = track.id == selectedTrackId,
                            onClick = {
                                onSelectTrack(track.id)
                                onDismiss()
                            }
                        )
                        if (index < tracks.lastIndex) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun TrackRow(
    title: String,
    lang: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) Color(0xFF8B5CF6) else Color.White,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = "[$lang]",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF8B5CF6),
                unselectedColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}
