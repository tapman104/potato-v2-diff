package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun PlayerQuickActions(
    decodeMode: DecodeMode,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onCycleDecodeMode: () -> Unit,
    onMoreOptions: () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    val buttonColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = Color.Black.copy(alpha = 0.35f),
        contentColor = Color.White
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandHorizontally(
                animationSpec = tween(180),
                expandFrom = Alignment.End
            ),
            exit = fadeOut(tween(180)) + shrinkHorizontally(
                animationSpec = tween(180),
                shrinkTowards = Alignment.End
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onSelectAudioTrack,
                    modifier = Modifier.size(40.dp).shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black),
                    colors = buttonColors
                ) {
                    Icon(
                        Icons.Filled.Audiotrack,
                        contentDescription = "Audio track",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSelectSubtitleTrack,
                    modifier = Modifier.size(40.dp).shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black),
                    colors = buttonColors
                ) {
                    Icon(
                        Icons.Filled.ClosedCaption,
                        contentDescription = "Subtitle track",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onCycleDecodeMode,
                    modifier = Modifier.size(40.dp).shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black),
                    colors = buttonColors
                ) {
                    Text(
                        text = when (decodeMode) {
                            DecodeMode.HW     -> "HW"
                            DecodeMode.HWPlus -> "HW+"
                            DecodeMode.SW     -> "SW"
                        },
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }

                FilledTonalIconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(40.dp).shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black),
                    colors = buttonColors
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Always-visible collapse/expand toggle
        FilledTonalIconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(48.dp).shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black),
            colors = buttonColors
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
