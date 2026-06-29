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
import com.tapman104.mpvplayer.player.model.AspectRatioMode

@Composable
fun AspectRatioDialog(
    currentMode: AspectRatioMode,
    onSelectMode: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = AspectRatioMode.values().toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        },
        title = {
            Text(
                text = "Aspect Ratio",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(modes) { index, mode ->
                    ModeRow(
                        title = mode.displayName,
                        isSelected = mode == currentMode,
                        onClick = {
                            onSelectMode(mode)
                            onDismiss()
                        }
                    )
                    if (index < modes.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ModeRow(
    title: String,
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
        Text(
            text = title,
            color = if (isSelected) Color(0xFF8B5CF6) else Color.White,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
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
