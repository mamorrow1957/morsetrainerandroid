# Morse Trainer

A single-screen Android app that fetches random Wikipedia articles and plays the first sentence as Morse code via the device speaker. Article content is hidden during transmission and only revealed afterwards, so you can practice decoding before seeing the answer.

*Vibe coded in Feb–Apr 2026 by Michael Morrow*

---

## Features

- **Random Wikipedia articles** — fetches a new article on each tap
- **Test mode** — article text is hidden during playback; revealed only when you tap "Reveal"
- **Learn mode** — each character appears in the text box in real time as its Morse code plays
- **Adjustable speed** — 10–50 WPM slider; changes take effect on the next symbol, even mid-transmission
- **Punctuation support** — transmits punctuation as standard Morse code sequences
- **Tappable source link** — opens the original Wikipedia article in your browser after reveal

## Requirements

- Android 9.0 (API 28) or higher
- Internet connection (Wikipedia API)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose (Material3) |
| Audio | `AudioTrack` (PCM, 44100 Hz mono, PCM_FLOAT) |
| Architecture | ViewModel + StateFlow |
| Networking | `HttpURLConnection` on `Dispatchers.IO` |

## How It Works

1. Tap **"Find an article"** — the app fetches a random Wikipedia article and immediately begins playing the first sentence as Morse code at 650 Hz.
2. In **Test mode**, the text box shows "Sending…" during playback. Try to decode the message.
3. In **Learn mode**, decoded characters appear in the text box character-by-character as they are transmitted.
4. Tap **"Stop sending"** at any time to halt playback early.
5. Tap **"Reveal"** to see the article title, sentence, and a tappable link to the full Wikipedia page.

## Speed

Speed is measured in Words Per Minute (WPM), from 10 to 50 WPM (default 30 WPM). Timing follows the PARIS standard:

```
unitSeconds = 60.0 / (cpm × 8.0)
```

| Element | Duration |
|---------|----------|
| Dot | 1 unit |
| Dash | 3 units |
| Intra-character gap | 1 unit |
| Inter-character gap | 3 units |
| Inter-word gap | 3.5 units (compressed for faster cadence) |

## Project Structure

```
app/src/main/kotlin/com/michaelmorrow/morsetrainer/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   └── Theme.kt
│   └── MorseTrainerScreen.kt
├── viewmodel/
│   └── MorseViewModel.kt
├── model/
│   └── ArticleModel.kt
└── service/
    ├── WikipediaService.kt
    ├── MorseEngine.kt
    └── SentenceExtractor.kt
```

## Building

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew test
```

Run UI tests (requires emulator or connected device):

```bash
./gradlew connectedAndroidTest
```

## License

Font: *1942 Report* by Johan Holmdahl (freeware).
