package com.michaelmorrow.morsetrainer.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michaelmorrow.morsetrainer.ui.theme.AppAccent
import com.michaelmorrow.morsetrainer.ui.theme.AppBackground
import com.michaelmorrow.morsetrainer.ui.theme.ControlSurface
import com.michaelmorrow.morsetrainer.ui.theme.HeaderText
import com.michaelmorrow.morsetrainer.ui.theme.Report1942Font
import com.michaelmorrow.morsetrainer.ui.theme.SurfaceBackground
import com.michaelmorrow.morsetrainer.viewmodel.AppState
import com.michaelmorrow.morsetrainer.viewmodel.Mode
import com.michaelmorrow.morsetrainer.viewmodel.MorseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MorseTrainerScreen(vm: MorseViewModel = viewModel()) {
    val appState by vm.appState.collectAsState()
    val displayText by vm.displayText.collectAsState()
    val articleUrl by vm.articleUrl.collectAsState()
    val wpm by vm.wpm.collectAsState()
    val mode by vm.mode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header()

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val availableHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(availableHeight * 0.03f))

                TextBox(
                    displayText = displayText,
                    articleUrl = articleUrl,
                    appState = appState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(availableHeight * 0.34f),
                )

                Spacer(modifier = Modifier.height(availableHeight * 0.03f))

                ControlsRow(
                    wpm = wpm,
                    mode = mode,
                    onWpmChange = { vm.setWpm(it) },
                    onModeChange = { vm.setMode(it) },
                )

                Spacer(modifier = Modifier.height(availableHeight * 0.03f))

                ActionButton(appState = appState, vm = vm)

                Spacer(modifier = Modifier.height(availableHeight * 0.19f))
            }
        }

        Footer()
    }
}

@Composable
private fun Header() {
    var titleText by remember { mutableStateOf("") }
    val fullTitle = "Morse Trainer"

    LaunchedEffect(Unit) {
        for (ch in fullTitle) {
            titleText += ch
            playClick()
            delay(80)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = titleText,
            fontFamily = Report1942Font,
            fontSize = 42.sp,
            color = HeaderText,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "titleLabel" },
        )
    }
}

@Composable
private fun Footer() {
    Text(
        text = "vibe coded in Feb–Apr 2026 by Michael Morrow",
        color = AppAccent,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(8.dp)
            .semantics { testTag = "footerLabel" },
    )
}

@Composable
private fun TextBox(
    displayText: String,
    articleUrl: String?,
    appState: AppState,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceBackground)
            .padding(12.dp)
            .semantics { testTag = "textbox" },
    ) {
        if (displayText.isEmpty()) {
            Text(
                text = "Press the button …",
                color = Color.Gray,
                fontSize = 16.sp,
            )
        } else if (appState == AppState.Idle && articleUrl != null) {
            val annotated = buildAnnotatedString {
                val lines = displayText.lines()
                lines.forEachIndexed { idx, line ->
                    when {
                        line.startsWith("Source: ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append("Source: ")
                            }
                            pushStringAnnotation(tag = "URL", annotation = articleUrl)
                            withStyle(
                                SpanStyle(
                                    color = Color.Blue,
                                    textDecoration = TextDecoration.Underline,
                                )
                            ) {
                                append(line.removePrefix("Source: "))
                            }
                            pop()
                        }
                        line.startsWith("Title: ") || line.startsWith("Sentence: ") -> {
                            val colon = line.indexOf(": ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append(line.substring(0, colon + 2))
                            }
                            withStyle(SpanStyle(color = Color.Black)) {
                                append(line.substring(colon + 2))
                            }
                        }
                        else -> withStyle(SpanStyle(color = Color.Black)) { append(line) }
                    }
                    if (idx < lines.lastIndex) append("\n")
                }
            }
            val scrollState = rememberScrollState()
            androidx.compose.foundation.text.ClickableText(
                text = annotated,
                style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
                modifier = Modifier.verticalScroll(scrollState),
                onClick = { offset ->
                    annotated.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                },
            )
        } else {
            val errorColor = if (displayText.startsWith("Error")) Color.Red else Color.Black
            val scrollState = rememberScrollState()
            Text(
                text = displayText,
                color = errorColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.verticalScroll(scrollState),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsRow(
    wpm: Int,
    mode: Mode,
    onWpmChange: (Int) -> Unit,
    onModeChange: (Mode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Learn / Test segmented picker
        val modes = listOf(Mode.Learn, Mode.Test)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .width(120.dp)
                .semantics { testTag = "modeSwitch" },
        ) {
            modes.forEachIndexed { idx, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { onModeChange(m) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AppAccent,
                        activeContentColor = Color.White,
                        activeBorderColor = AppAccent,
                        inactiveContainerColor = ControlSurface,
                        inactiveContentColor = Color.White,
                        inactiveBorderColor = ControlSurface,
                    ),
                    label = {
                        Text(
                            text = if (m == Mode.Learn) "Learn" else "Test",
                            fontSize = 13.sp,
                        )
                    },
                    icon = {},
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Speed slider + label
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Speed: $wpm WPM",
                color = AppAccent,
                fontSize = 13.sp,
                modifier = Modifier.semantics { testTag = "speedLabel" },
            )
            Slider(
                value = wpm.toFloat(),
                onValueChange = { onWpmChange(it.toInt()) },
                valueRange = 10f..50f,
                steps = 39,
                colors = SliderDefaults.colors(
                    thumbColor = AppAccent,
                    activeTrackColor = AppAccent,
                    inactiveTrackColor = ControlSurface,
                ),
                modifier = Modifier.semantics { testTag = "speedSlider" },
            )
        }
    }
}

@Composable
private fun ActionButton(appState: AppState, vm: MorseViewModel) {
    val (label, enabled) = when (appState) {
        AppState.Idle -> "Find an article" to true
        AppState.Loading -> "Loading…" to false
        AppState.Sending -> "Stop sending" to true
        AppState.Reveal -> "Reveal" to true
    }

    Button(
        onClick = {
            when (appState) {
                AppState.Idle -> vm.onFindArticle()
                AppState.Sending -> vm.onStopSending()
                AppState.Reveal -> vm.onReveal()
                AppState.Loading -> {}
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceBackground,
            contentColor = Color.Black,
            disabledContainerColor = SurfaceBackground.copy(alpha = 0.7f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .semantics { testTag = "findBtn" },
    ) {
        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

// Short mechanical click sound for the typewriter animation
private suspend fun playClick() = withContext(Dispatchers.Default) {
    val sampleRate = 44100
    val samples = sampleRate / 200  // 5 ms
    val buffer = FloatArray(samples) { i ->
        val t = i.toDouble() / sampleRate
        (sin(2.0 * PI * 880.0 * t) * 0.3 * (1.0 - i.toDouble() / samples)).toFloat()
    }
    val bufSize = AudioTrack.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
    ).coerceAtLeast(buffer.size * 4)
    val track = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    } catch (_: Exception) {
        return@withContext
    }
    if (track.state == AudioTrack.STATE_UNINITIALIZED) { track.release(); return@withContext }
    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
    track.play()
    delay(20)
    runCatching { track.stop() }
    track.release()
}
