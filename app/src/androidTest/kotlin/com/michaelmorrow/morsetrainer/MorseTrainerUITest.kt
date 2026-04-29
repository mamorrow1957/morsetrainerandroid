package com.michaelmorrow.morsetrainer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelmorrow.morsetrainer.ui.MorseTrainerScreen
import com.michaelmorrow.morsetrainer.ui.theme.MorseTrainerTheme
import com.michaelmorrow.morsetrainer.viewmodel.MorseViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MorseTrainerUITest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launch(vm: MorseViewModel = MorseViewModel()) {
        composeRule.setContent {
            MorseTrainerTheme {
                MorseTrainerScreen(vm = vm)
            }
        }
    }

    // ── 11.3 Launch tests ──────────────��───────────────────────────────────────

    @Test
    fun appLaunchesWithoutCrashing() {
        launch()
        composeRule.onNodeWithTag("textbox").assertIsDisplayed()
    }

    @Test
    fun titleLabelContainsMorseTrainer() {
        launch()
        // Wait for typewriter animation to finish
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("titleLabel").assertTextContains("Morse Trainer")
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun footerLabelContainsAttribution() {
        launch()
        composeRule.onNodeWithTag("footerLabel").assertTextContains("Michael Morrow")
    }

    @Test
    fun textBoxIsVisibleWithPlaceholder() {
        launch()
        composeRule.onNodeWithTag("textbox").assertIsDisplayed()
        composeRule.onNodeWithText("Press the button …").assertIsDisplayed()
    }

    // ── 11.4 Speed slider tests ────────────────────────────────────────────────

    @Test
    fun speedSliderIsPresent() {
        launch()
        composeRule.onNodeWithTag("speedSlider").assertIsDisplayed()
    }

    @Test
    fun speedLabelShowsDefaultWpm() {
        launch()
        composeRule.onNodeWithTag("speedLabel").assertTextContains("30")
    }

    // ── 11.5 Button tests ─────────────────────��────────────────────────────────

    @Test
    fun findArticleButtonIsVisibleOnLaunch() {
        launch()
        composeRule.onNodeWithTag("findBtn").assertIsDisplayed()
        composeRule.onNodeWithText("Find an article").assertIsDisplayed()
    }

    @Test
    fun buttonShowsLoadingAfterTap() {
        launch()
        composeRule.onNodeWithTag("findBtn").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            runCatching {
                composeRule.onNodeWithText("Loading…").assertIsDisplayed()
                true
            }.getOrDefault(false) || runCatching {
                composeRule.onNodeWithText("Stop sending").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    // ── 11.6 Morse playback tests ───────────────────��──────────────────────────

    @Test
    fun morseDoneBecomesTrue() {
        val vm = MorseViewModel()
        launch(vm)
        composeRule.onNodeWithTag("findBtn").performClick()
        composeRule.waitUntil(timeoutMillis = 60_000) {
            vm.morseDone.value
        }
        assert(vm.morseDone.value)
    }

    // ── 11.8 Stop sending / Reveal flow ───────────────────��───────────────────

    @Test
    fun stopSendingShowsRevealButton() {
        launch()
        composeRule.onNodeWithTag("findBtn").performClick()
        // Wait until sending
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Stop sending").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Stop sending").performClick()
        composeRule.onNodeWithText("Reveal").assertIsDisplayed()
        composeRule.onNodeWithTag("textbox").assertTextContains("Stopped")
    }

    @Test
    fun revealPopulatesArticleLines() {
        val vm = MorseViewModel()
        launch(vm)
        composeRule.onNodeWithTag("findBtn").performClick()
        // Wait for playback to complete or stop it early
        composeRule.waitUntil(timeoutMillis = 60_000) {
            vm.morseDone.value
        }
        composeRule.onNodeWithText("Reveal").performClick()
        composeRule.onNodeWithTag("textbox").assertTextContains("Title:")
        composeRule.onNodeWithTag("textbox").assertTextContains("Sentence:")
        composeRule.onNodeWithTag("textbox").assertTextContains("Source:")
        composeRule.onNodeWithText("Find an article").assertIsDisplayed()
    }

    @Test
    fun buttonNeverShowsStopSendingWhenIdle() {
        launch()
        // On launch no "Stop sending" button should be present
        composeRule.onNodeWithText("Stop sending").assertDoesNotExist()
    }

    // ── 11.7 Learn mode tests ──────────────────���───────────────────────────────

    @Test
    fun modeSwitcherIsPresent() {
        launch()
        composeRule.onNodeWithTag("modeSwitch").assertIsDisplayed()
    }

    @Test
    fun defaultModeIsTest() {
        val vm = MorseViewModel()
        launch(vm)
        assert(vm.mode.value == com.michaelmorrow.morsetrainer.viewmodel.Mode.Test)
    }

    @Test
    fun testModeShowsSendingDuringPlayback() {
        launch()
        composeRule.onNodeWithTag("findBtn").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Stop sending").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("textbox").assertTextContains("Sending")
    }
}
