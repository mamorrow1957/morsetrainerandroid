# Morse Trainer Android — Project Specifications

---

## 1. Project Overview

**Morse Trainer Android** is a single-screen Jetpack Compose application that fetches random Wikipedia articles and plays the first sentence as Morse code via the device speaker. The article content is deliberately withheld during transmission and only revealed afterwards, so the user can practise decoding the code before seeing the answer. The user can adjust playback speed in real time, even while code is being sent.

**App display name (launcher icon label):** Morse Trainer (`android:label` in `AndroidManifest.xml`)

**Platform:** Android 9.0 (API 28)+
**Language:** Kotlin
**UI Framework:** Jetpack Compose (Material3)
**Audio Framework:** `AudioTrack` (PCM, `android.media.AudioTrack`)

---

## 2. Project Structure

```
MorseTrainerAndroid/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/michaelmorrow/morsetrainer/
│       │   │   ├── MainActivity.kt                  # App entry point
│       │   │   ├── ui/
│       │   │   │   └── MorseTrainerScreen.kt        # Root composable screen
│       │   │   ├── viewmodel/
│       │   │   │   └── MorseViewModel.kt            # App state and business logic
│       │   │   ├── model/
│       │   │   │   └── ArticleModel.kt              # Wikipedia article data model
│       │   │   └── service/
│       │   │       ├── WikipediaService.kt          # Article fetching
│       │   │       ├── MorseEngine.kt               # Morse code playback engine
│       │   │       └── SentenceExtractor.kt         # Sentence extraction logic
│       │   └── res/
│       │       ├── font/
│       │       │   └── report1942.ttf               # 1942 Report custom font (Johan Holmdahl, freeware)
│       │       └── values/
│       │           ├── colors.xml
│       │           └── strings.xml
│       ├── test/
│       │   └── kotlin/com/michaelmorrow/morsetrainer/
│       │       ├── MorseViewModelTest.kt            # JUnit unit tests
│       │       ├── SentenceExtractorTest.kt
│       │       └── MorseEngineTest.kt
│       └── androidTest/
│           └── kotlin/com/michaelmorrow/morsetrainer/
│               └── MorseTrainerUITest.kt            # Compose UI tests
└── build.gradle.kts
```

---

## 3. Screen Layout

### 3.1 Overall Structure

The screen is a single `Column` divided into three regions that together fill the entire display:

| Region | Compose Element | Background Color |
|--------|----------------|-----------------|
| Header | `Column` / `Text` | Very dark grey (`#1a1a1a`) |
| Main Content | `Column` (fills remaining space via `Modifier.weight(1f)`) | Very dark grey (`#1a1a1a`) |
| Footer | `Column` / `Text` | Very dark grey (`#1a1a1a`) |

The screen background must be uniformly dark grey with no visible dividers between regions. The root composable must consume `WindowInsets.systemBars` padding (`Modifier.safeDrawingPadding()` or equivalent) so the layout respects the status bar and navigation bar on all device sizes.

### 3.2 Header

- Contains the app title: **"Morse Trainer"** with a typewriter character-by-character animation on launch, accompanied by mechanical click sounds
- Text color: `#e0e0e0` (very light grey)
- Font: **1942 Report** (`report1942`) custom font loaded via `FontFamily(Font(R.font.report1942))`, auto-scaled to match the width of the text box using `autoSize` text or `adjustsFontSizeToFit`-equivalent logic
- Centered horizontally with horizontal padding of `20.dp` to align with the text box edges

### 3.3 Footer

- Contains the attribution string: **"vibe coded in Feb–Apr 2026 by Michael Morrow"**
- Text color: `#ff4d00`
- Centered, small font (`fontSize = 12.sp`)

### 3.4 Main Content Area

The main content region holds the text box, the Learn/Test mode picker, the speed control, and the button. It uses a `BoxWithConstraints` wrapping a `Column`, with all spacing and sizing expressed as proportions of the available height so the layout scales correctly across all Android phone and tablet sizes:

| Element | Size |
|---------|------|
| Text box height | 34% of available height |
| Gap between text box and controls | 3% of available height |
| Gap between controls and button | 3% of available height |
| Bottom spacer (pushes content above centre) | 19% of available height |

---

## 4. Text Box

| Property | Value |
|----------|-------|
| Background | Very light grey (`#e8e8e8`) |
| Text color | Black |
| Width | Full width with horizontal padding (`Modifier.fillMaxWidth()`) |
| Height | 34% of the main content area's available height (proportional, via `BoxWithConstraints`) |
| Corner radius | 8 dp (`RoundedCornerShape(8.dp)`) |
| Font size | Body — large enough to comfortably display several rows of text (`16.sp`) |
| Placeholder text | **"Press the button …"** — shown when content is empty |
| Scrolling | Content is wrapped in a `Column` with `Modifier.verticalScroll(rememberScrollState())`; text scrolls vertically if it overflows |

The text box is read-only (display only). It must be capable of rendering a tappable hyperlink on its third line when in Reveal state (see §8 — Article Display). The Source line is rendered as a composed `AnnotatedString` with a `SpanStyle(color = Color.Blue)` URL span and a `ClickableText` composable whose `onClick` resolves the URL span and calls `uriHandler.openUri(url)` via `LocalUriHandler.current`.

### 4.1 Text Box States

| State | Content |
|-------|---------|
| Idle (initial launch or after Reveal) | Placeholder text: **"Press the button …"** |
| Sending (Test mode) | **"Sending …"** |
| Sending (Learn mode) | Accumulated decoded characters, updated in real time |
| Finished (Test mode) | **"Send complete…"** |
| Finished (Learn mode) | Decoded sentence remains visible — not replaced with "Send complete…" |
| Stopped | **"Stopped …"** |
| Error | **"Error fetching article: \<reason\>"** in red |
| Reveal | The three article lines (Title, Sentence, Source) as described in §8 |

---

## 5. Learn/Test Mode Picker

A segmented control placed to the **left of the speed slider**, in a `Row` within the controls area.

| Property | Value |
|----------|-------|
| Compose element | `SingleChoiceSegmentedButtonRow` (Material3) |
| Width | Fixed at `120.dp` |
| Choices | **"Learn"** and **"Test"** |
| Default | **"Test"** |
| Selected segment color | `#ff4d00` (orange) — always, including during playback (`selectedContainerColor` in `SegmentedButtonDefaults.colors()`) |
| Non-selected segment background | Dark grey (`Color(0xFF404040)`) with white text |
| Label font | `MaterialTheme.typography.labelMedium` |

The picker is **never visually disabled**. Instead, mode changes triggered during active playback are silently rejected in `MorseViewModel` (the `mode` setter restores the previous value if `appState == AppState.Sending` or `AppState.Loading`). This prevents the selected segment from changing colour during playback.

### 5.1 Test Mode (default)

When set to **Test**, article content is hidden during Morse playback and revealed only when the user taps "Reveal" (or "Stop sending").

### 5.2 Learn Mode

When set to **Learn**, each character is revealed to the user **as its Morse code is transmitted** at the currently selected speed:

- The text box **starts empty** when playback begins (no "Sending …" message in Learn mode).
- Decoded characters are appended to the text box in real time, in their **original sentence case** (not uppercased).
- Each character is appended **as its first Morse symbol (dit or dah) begins playing**.
- Word spaces are appended at the start of the inter-word gap, triggered by the same `onCharacterStart` callback used for letters.
- When playback completes naturally, the decoded sentence **remains in the text box** (it is not replaced with "Send complete…").
- **Stop and Reveal** behave identically to Test mode.

---

## 6. Controls Area

The controls area sits below the text box, grouped in a `Column`, horizontally centred.

### 6.1 Speed Slider

| Property | Value |
|----------|-------|
| Compose element | `Slider` |
| Minimum | 10 WPM (= 50 CPM) |
| Maximum | 50 WPM (= 250 CPM) |
| Default | 30 WPM (= 150 CPM) |
| Step | 1 WPM (`steps = 39` for 40 intervals) |
| Active track color | `#ff4d00` (set via `SliderDefaults.colors(activeTrackColor = ...)`) |
| Inactive track color | Dark grey (`Color(0xFF404040)`) to match the mode picker non-selected background |
| Label color | `#ff4d00` |
| Label font | `MaterialTheme.typography.labelMedium` |
| Live readout | A `Text` composable above the slider displaying **"Speed: \<value\> WPM"** |

Speed is stored as WPM and converted to CPM (×5) when passed to `MorseEngine`. The slider value is readable at any time, including during playback. Changing the slider mid-transmission takes effect on the next Morse symbol (see §9.3).

### 6.2 Button

| Property | Value |
|----------|-------|
| Width | ~60% of screen width (`Modifier.fillMaxWidth(0.6f)`) |
| Background | Very light grey (`#e8e8e8`) |
| Text color | Black |
| Font size | Large enough to fill the button visually (`18.sp`) |
| Corner radius | 8 dp (`RoundedCornerShape(8.dp)`) |
| Horizontal position | Centred (`Alignment.CenterHorizontally`) |

The button cycles through four states:

| State | Label | Action on tap |
|-------|-------|---------------|
| **Idle** | **"Find an article"** | Fetch article, begin Morse playback, enter Sending state |
| **Sending** | **"Stop sending"** | Immediately halt playback and enter Reveal state |
| **Reveal** | **"Reveal"** | Populate text box with article content (§8), enter Idle state |
| *(loading)* | **"Loading…"** | Disabled — fetch in progress |

State transition rules:
1. On launch the button is in **Idle** state.
2. Tapping **"Find an article"** fetches the article (button shows "Loading…" and is disabled), then begins Morse playback and switches to **Sending**.
3. When Morse playback completes naturally, the text box shows "Send complete…" and the button advances to **Reveal**.
4. Tapping **"Stop sending"** immediately halts playback, shows "Stopped …", and advances to **Reveal**.
5. Tapping **"Reveal"** populates the text box with the three article lines (§8) and returns to **Idle**.
6. The button must **never** show **"Stop sending"** when no Morse playback is in progress.

---

## 7. Wikipedia Article Fetching

Implemented in `WikipediaService.kt` using `java.net.HttpURLConnection` (or `OkHttp` if already a project dependency) on a background coroutine dispatcher (`Dispatchers.IO`).

When "Find an article" is tapped:

1. A **random English Wikipedia article** is fetched via:
   ```
   https://en.wikipedia.org/api/rest_v1/page/random/summary
   ```
2. The article title, extracted sentence, and URL are decoded from the JSON response and stored in `ArticleModel`.
3. Morse playback of the extracted sentence begins immediately after a successful fetch.
4. Article content is **not shown** until the user taps "Reveal".

The fetch must have a **10-second timeout** (`connectTimeout = 10_000`, `readTimeout = 10_000`). On failure:

- Display **"Error fetching article: \<reason\>"** in red in the text box.
- Return the button to **"Find an article"** (Idle state).
- Set `appState = AppState.Idle` in the view model.

The `AndroidManifest.xml` must include `<uses-permission android:name="android.permission.INTERNET" />`.

---

## 8. Article Display

When the user taps **"Reveal"** (or **"Stop sending"**, which triggers Reveal immediately), the text box is populated with exactly three lines:

| Line | Format |
|------|--------|
| Line 1 | `Title: <article title>` |
| Line 2 | `Sentence: <first complete sentence from the article content>` |
| Line 3 | `Source: <article URL>` — rendered as tappable blue text that opens in the device's default browser via `LocalUriHandler.current.openUri(url)` |

The Source URL must open in the device's default browser. Implemented as a `ClickableText` composable with an `AnnotatedString` containing a `LinkAnnotation.Url` (or `StringAnnotation` with tag `"URL"`) span on the URL portion, styled in blue.

### 8.1 Sentence Extraction Rules

Implemented in `SentenceExtractor.kt`.

- The target sentence is the **first complete sentence** found in the article's `extract` field from the Wikipedia API response.
- A sentence ends with `.`, `!`, or `?` followed by a space or end-of-string.
- Sentences must be **longer than 5 words**.
- **Abbreviations must not be treated as sentence terminators.** Excluded: titles (`Dr.`, `Mr.`, `Mrs.`, `Ms.`, `Prof.`), common terms (`vs.`, `etc.`, `Jr.`, `Sr.`, `Fig.`, `No.`, `St.`, `Ave.`, `Blvd.`, `Dept.`, `Est.`, `Approx.`), corporate suffixes (`Inc.`, `Ltd.`, `Corp.`), honorifics (`Gov.`, `Gen.`, `Col.`, `Sgt.`, `Cpl.`, `Pvt.`, `Rep.`, `Sen.`, `Rev.`), months (`Jan.`–`Dec.`), days (`Mon.`–`Sun.`), academic/Latin abbreviations (`Vol.`, `pp.`, `ed.`, `al.`, `ie.`, `eg.`, `op.`, `ca.`, `cf.`, `et.`), and single capital-letter initials (e.g. `J.`).
- Strip any residual wiki markup, citation brackets (e.g., `[1]`), or HTML tags before display.
- **Fallback:** If no qualifying sentence is found, use the first 250 characters of the cleaned text.

---

## 9. Morse Code Playback

Implemented in `MorseEngine.kt`.

### 9.1 Timing Basis

Morse timing is derived from the **CPM** value on the speed slider:

```
unitSeconds = 60.0 / (cpm.toDouble() * 8.0)
```

*(Consistent with the PARIS standard — approximately 6 units per average character.)*

### 9.2 Element Durations

| Element | Duration |
|---------|----------|
| Dot | 1 unit |
| Dash | 3 units |
| Intra-character gap (between dot/dash) | 1 unit |
| Inter-character gap (between letters) | 3 units |
| Inter-word gap | 7 units (halved to 3.5 units per original spec note — see below) |

> **Note:** Inter-word gaps are shortened to half the standard 7-unit word space for a faster, more compressed playback cadence (matching the web version behaviour).

### 9.3 Real-Time Speed Adjustment

- The CPM value is sampled **per symbol** (each dot, dash, or gap).
- Moving the slider during transmission takes effect on the next scheduled symbol without interrupting the current one.
- No audio glitches or gaps should result from a mid-playback speed change.

### 9.4 Audio Implementation

- Use `AudioTrack` in streaming mode (`AudioTrack.MODE_STREAM`) with programmatically generated PCM audio buffers written via `audioTrack.write(buffer, 0, size)` on a dedicated background thread or coroutine.
- Set `AudioAttributes` with usage `USAGE_MEDIA` and content type `CONTENT_TYPE_MUSIC` so audio plays even when the device ringer is on silent.
- PCM buffer format: **44100 Hz, mono, PCM_FLOAT** (`AudioFormat.ENCODING_PCM_FLOAT`, `AudioFormat.CHANNEL_OUT_MONO`). This format must be used consistently — a mismatch causes distorted or silent output.
- Tone frequency: **650 Hz** (standard Morse sidetone).
- No other sounds are played before or between Morse characters.
- If `AudioTrack` initialisation fails (state `== AudioTrack.STATE_UNINITIALIZED`), playback falls back to a timed delay (silent mode) using `kotlinx.coroutines.delay` so the rest of the UI still functions.

### 9.5 Playback Completion Signal

- When playback finishes (naturally or via "Stop sending"), the view model sets `appState = AppState.Reveal` on the **main thread** (`withContext(Dispatchers.Main)`).
- A `val morseDone: StateFlow<Boolean>` property on the view model is set to `true` on completion and reset to `false` at the start of each new playback session.
- UI tests use this state flow (observed via `collectAsState()`) to detect completion.

### 9.6 Playback Trigger

Morse plays the extracted sentence immediately after a successful fetch. No additional user interaction is required to start playback after tapping "Find an article".

### 9.7 Punctuation in Playback

Punctuation present in the sentence must be transmitted as Morse code:

| Character | Morse |
|-----------|-------|
| `.` | `.-.-.-` |
| `,` | `--..--` |
| `?` | `..--..` |
| `'` | `.----.` |
| `!` | `-.-.--` |
| `/` | `-..-.` |
| `(` | `-.--.` |
| `)` | `-.--.-` |
| `&` | `.-...` |
| `:` | `---...` |
| `;` | `-.-.-.` |
| `=` | `-...-` |
| `+` | `.-.-.` |
| `-` | `-....-` |
| `_` | `..--.-` |
| `"` | `.-..-.` |
| `$` | `...-..-` |
| `@` | `.--.-.` |

Any punctuation not in this table is silently skipped.

---

## 10. Visual Design

- Background: `#1a1a1a` (very dark grey) — applied as the app's global background via `Modifier.background(AppBackground)` on the root `Column`
- Accent / label color: `#ff4d00` (orange-red)
- Text box and button background: `#e8e8e8` (very light grey)
- Text box and button text: `Color.Black`
- Define colors as Compose `Color` constants in a `Color.kt` file (and optionally in `res/values/colors.xml` for the splash/system UI):
  - `AppBackground` → `Color(0xFF1A1A1A)`
  - `AppAccent` → `Color(0xFFFF4D00)`
  - `SurfaceBackground` → `Color(0xFFE8E8E8)`
- The status bar and navigation bar must use the dark background color. Set via `WindowCompat.setDecorFitsSystemWindows(window, false)` and `SystemUiController` (Accompanist) or `WindowInsetsControllerCompat`.
- The layout must be usable on all current Android phone screen sizes in portrait orientation.
- **Phone is locked to portrait only** (`android:screenOrientation="portrait"` in `<activity>` in `AndroidManifest.xml`).
- **Tablets (sw600dp+) support all orientations.** In portrait, the layout matches phone. In landscape, a two-column layout is used: the left column (62% width) holds the text box; the right column (38% width) holds the controls and button. The header and footer span the full width above and below the columns respectively. Use `LocalConfiguration.current.screenWidthDp >= 600` and `isLandscape` checks to switch between layouts.
- Support Dynamic Type / font scaling for accessibility (`sp` units for all text sizes).

---

## 11. Test Suite

### 11.1 Unit Tests (`test/` source set, JUnit 4)

All business logic in `MorseViewModel`, `SentenceExtractor`, and `MorseEngine` must be covered by unit tests using JUnit 4 (`@Test`, `@Before`). Use `kotlinx-coroutines-test` (`TestCoroutineDispatcher` / `runTest`) for coroutine-based logic.

### 11.2 UI Tests (`androidTest/` source set, Compose Testing)

UI tests use `createComposeRule()` (`ComposeTestRule`) and interact with the UI via test tags (see §12). These tests run on an emulator or physical device.

### 11.3 Launch Tests

- App launches without crashing.
- Title node with test tag `"titleLabel"` contains "Morse Trainer".
- Footer node with test tag `"footerLabel"` contains the attribution string.
- Text box node with test tag `"textbox"` is displayed and shows placeholder text.

### 11.4 Speed Slider Tests

- Slider node with test tag `"speedSlider"` is present with default value `30` (WPM).
- Speed readout node with test tag `"speedLabel"` displays `30` on launch.
- Performing a swipe on the slider updates the readout in real time.

### 11.5 Button Tests

- "Find an article" button (test tag `"findBtn"`) is visible and labeled correctly on launch.
- Tapping "Find an article" transitions the button to "Loading…" then "Stop sending".
- After playback completes (`morseDone` becomes `true`), button label changes to "Reveal".
- Tapping "Reveal" causes the text box to contain text beginning `Title:`.
- Tapping "Reveal" causes the text box to contain text beginning `Sentence:`.
- Tapping "Reveal" causes the text box to contain text beginning `Source:` with a valid URL.
- Tapping "Reveal" returns the button label to "Find an article".

### 11.6 Morse Playback Tests

- After tapping "Find an article", `morseDone` eventually becomes `true` (allow up to 30 s using `waitUntil(timeoutMillis = 30_000)`).
- Adjusting the slider mid-playback does not throw an error.
- `morseDone` is reset to `false` at the start of each new playback session.

### 11.7 Speed Change During Playback

- Start playback, programmatically set the slider value mid-transmission; verify no crash and `morseDone` still becomes `true`.

### 11.8 Stop Sending / Reveal Flow

- While sending, button label is "Stop sending".
- Tapping "Stop sending" halts playback immediately, sets text box to "Stopped …", and button to "Reveal".
- Tapping "Stop sending" sets `morseDone` to `true` immediately.
- When playback completes naturally, text box shows "Send complete…" and button shows "Reveal".
- Tapping "Reveal" after natural completion populates `Title:`, `Sentence:`, and `Source:` lines.
- Tapping "Reveal" after "Stop sending" populates the same three lines.
- After "Reveal", button returns to "Find an article".
- Button never shows "Stop sending" when no playback is in progress.

### 11.9 Learn Mode Tests

- Mode picker node with test tag `"modeSwitch"` is present.
- Default selection is **Test**.
- In Test mode, text box shows "Sending …" during playback (not decoded characters).
- Switching to Learn mode and tapping "Find an article" causes plaintext characters to appear in the text box during playback.
- In Learn mode, `morseDone` still becomes `true` after playback completes.
- In Learn mode, the decoded sentence remains in the text box after playback completes (not replaced with "Send complete…").
- In Learn mode, tapping "Stop sending" halts playback and advances to Reveal state.
- In Learn mode, tapping "Reveal" populates the full `Title:`, `Sentence:`, and `Source:` lines.

---

## 12. Key ViewModel Properties and Test Tags

| Property / Test Tag | Type | Purpose |
|---------------------|------|---------|
| `appState` | `StateFlow<AppState>` | `AppState.Idle`, `.Loading`, `.Sending`, `.Reveal` |
| `morseDone` | `StateFlow<Boolean>` | Signals playback completion to tests |
| `mode` | `StateFlow<Mode>` | `Mode.Learn`, `Mode.Test` |
| `wpm` | `StateFlow<Int>` | Current speed in WPM (converted to CPM ×5 for engine) |
| `displayText` | `StateFlow<String>` | Text box content |
| `"textbox"` | Test tag | Text display area |
| `"modeSwitch"` | Test tag | Learn/Test picker |
| `"speedSlider"` | Test tag | WPM slider |
| `"speedLabel"` | Test tag | Live WPM readout |
| `"findBtn"` | Test tag | Primary action button |
| `"titleLabel"` | Test tag | Header title text |
| `"footerLabel"` | Test tag | Footer attribution text |

Test tags are applied via `Modifier.semantics { testTag = "..." }` (or `Modifier.testTag("...")`) on the corresponding composables.

---

*End of Specifications*
