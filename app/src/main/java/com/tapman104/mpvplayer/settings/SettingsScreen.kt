package com.tapman104.mpvplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    preferredSubtitleLang: String,
    onSubtitleLangChange: (String) -> Unit,
    subtitleSize: Float,
    subtitlePosition: Float,
    onSubtitleSizeChange: (Float) -> Unit,
    onSubtitlePositionChange: (Float) -> Unit,
    resumePlayback: Boolean,
    onResumePlaybackChange: (Boolean) -> Unit,
    decodeMode: String,
    onDecodeModeChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSubtitleAppearance by remember { mutableStateOf(false) }

    if (showSubtitleAppearance) {
        SubtitleAppearanceSection(
            subtitleSize = subtitleSize,
            subtitlePosition = subtitlePosition,
            onSizeChange = onSubtitleSizeChange,
            onPositionChange = onSubtitlePositionChange,
            onBack = { showSubtitleAppearance = false }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF111111))
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Section: SUBTITLES
            Text(
                text = "SUBTITLES",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 6.dp)
            )

            // Row 1 - Preferred Language
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        val langs = listOf("en", "jpn", "kor", "none")
                        val currentIndex = langs.indexOf(preferredSubtitleLang)
                        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % langs.size
                        onSubtitleLangChange(langs[nextIndex])
                    }
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Subtitle Language",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preferredSubtitleLang,
                        color = Color(0xFF8B5CF6),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2 - Subtitle Appearance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { showSubtitleAppearance = true }
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.TextFields,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Subtitle Appearance",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

            // Section: PLAYBACK
            Text(
                text = "PLAYBACK",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 6.dp)
            )

            // Row 3 - Default Decode Mode
            val decodeModes = listOf("mediacodec-copy", "mediacodec", "no")
            val decodeModeLabel = when (decodeMode) {
                "mediacodec-copy" -> "HW+"
                "mediacodec" -> "HW"
                "no" -> "SW"
                else -> "SW"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        val currentIndex = decodeModes.indexOf(decodeMode)
                        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % decodeModes.size
                        onDecodeModeChange(decodeModes[nextIndex])
                    }
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Default Decode Mode",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = decodeModeLabel,
                    color = Color(0xFF8B5CF6),
                    fontSize = 13.sp
                )
            }

            // Row 4 - Resume Playback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { onResumePlaybackChange(!resumePlayback) }
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Resume Playback",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
                Switch(
                    checked = resumePlayback,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF8B5CF6)
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

            // Section: ABOUT
            Text(
                text = "ABOUT",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 6.dp)
            )

            // About Rows
            val aboutItems = listOf(
                "App" to "Potato Player",
                "Version" to "1.0",
                "Engine" to "libmpv",
                "Source" to "github.com/tapman104"
            )

            aboutItems.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = value,
                        color = Color(0xFF8B5CF6),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
