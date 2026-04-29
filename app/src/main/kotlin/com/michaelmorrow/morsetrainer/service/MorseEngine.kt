package com.michaelmorrow.morsetrainer.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

class MorseEngine {

    private val sampleRate = 44100
    private val toneFreq = 650.0
    private val amplitude = 0.7f

    private val morseMap = mapOf(
        'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
        'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
        'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
        'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
        'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
        'Y' to "-.--",  'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-",
        '&' to ".-...",  ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.",  '-' to "-....-", '_' to "..--..", '"' to ".-..-.",
        '$' to "...-..-",'@' to ".--.-.",
    )

    suspend fun play(
        sentence: String,
        getCpm: () -> Int,
        onCharacterStart: (Char) -> Unit,
    ) = withContext(Dispatchers.Default) {
        val track = createAudioTrack()
        if (track == null) {
            playWithDelays(sentence, getCpm, onCharacterStart)
            return@withContext
        }

        track.play()
        var completedNaturally = false
        try {
            for (originalChar in sentence) {
                ensureActive()
                if (originalChar == ' ') {
                    onCharacterStart(' ')
                    // Extra silence to bring total word gap to 3.5 units
                    // (3 units of inter-char gap from previous letter + 0.5 here)
                    writeToTrack(track, silence(unitSamples(getCpm()) / 2))
                } else {
                    val code = morseMap[originalChar.uppercaseChar()] ?: continue
                    var isFirst = true
                    for ((idx, sym) in code.withIndex()) {
                        ensureActive()
                        val unit = unitSamples(getCpm())
                        if (isFirst) {
                            onCharacterStart(originalChar)
                            isFirst = false
                        }
                        when (sym) {
                            '.' -> writeToTrack(track, tone(unit))
                            '-' -> writeToTrack(track, tone(unit * 3))
                        }
                        if (idx < code.lastIndex) writeToTrack(track, silence(unit))
                    }
                    // Inter-character gap: 3 units
                    writeToTrack(track, silence(unitSamples(getCpm()) * 3))
                }
            }
            completedNaturally = true
        } finally {
            if (!completedNaturally) {
                runCatching { track.pause() }
                runCatching { track.flush() }
            } else {
                runCatching { delay(300) }
            }
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun unitSamples(cpm: Int): Int {
        val unitSeconds = 60.0 / (cpm.toDouble() * 8.0)
        return (unitSeconds * sampleRate).toInt().coerceAtLeast(1)
    }

    private fun tone(samples: Int): FloatArray {
        return FloatArray(samples) { i ->
            (sin(2.0 * PI * toneFreq * i / sampleRate) * amplitude).toFloat()
        }
    }

    private fun silence(samples: Int): FloatArray = FloatArray(samples.coerceAtLeast(1))

    private suspend fun writeToTrack(track: AudioTrack, samples: FloatArray) {
        var offset = 0
        while (offset < samples.size) {
            currentCoroutineContext().ensureActive()
            val written = track.write(
                samples, offset, samples.size - offset, AudioTrack.WRITE_NON_BLOCKING,
            )
            when {
                written > 0 -> offset += written
                written == 0 -> delay(4)
                else -> return
            }
        }
    }

    private fun createAudioTrack(): AudioTrack? {
        return try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                track.release()
                null
            } else {
                track
            }
        } catch (_: Exception) {
            null
        }
    }

    // Silent-mode fallback: timing only, no audio
    private suspend fun playWithDelays(
        sentence: String,
        getCpm: () -> Int,
        onCharacterStart: (Char) -> Unit,
    ) {
        for (originalChar in sentence) {
            currentCoroutineContext().ensureActive()
            val cpm = getCpm()
            val unitMs = (60_000.0 / (cpm * 8.0)).toLong().coerceAtLeast(1L)

            if (originalChar == ' ') {
                onCharacterStart(' ')
                delay(unitMs / 2)
            } else {
                val code = morseMap[originalChar.uppercaseChar()] ?: continue
                var isFirst = true
                for ((idx, sym) in code.withIndex()) {
                    if (!currentCoroutineContext().isActive) return
                    if (isFirst) { onCharacterStart(originalChar); isFirst = false }
                    when (sym) {
                        '.' -> delay(unitMs)
                        '-' -> delay(unitMs * 3)
                    }
                    if (idx < code.lastIndex) delay(unitMs)
                }
                delay(unitMs * 3)
            }
        }
    }
}
