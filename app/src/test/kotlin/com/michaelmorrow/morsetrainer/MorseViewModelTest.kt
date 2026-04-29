package com.michaelmorrow.morsetrainer

import com.michaelmorrow.morsetrainer.viewmodel.AppState
import com.michaelmorrow.morsetrainer.viewmodel.Mode
import com.michaelmorrow.morsetrainer.viewmodel.MorseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MorseViewModelTest {

    private lateinit var vm: MorseViewModel

    @Before
    fun setUp() {
        vm = MorseViewModel()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(AppState.Idle, vm.appState.value)
    }

    @Test
    fun `initial mode is Test`() {
        assertEquals(Mode.Test, vm.mode.value)
    }

    @Test
    fun `initial wpm is 30`() {
        assertEquals(30, vm.wpm.value)
    }

    @Test
    fun `initial displayText is empty`() {
        assertEquals("", vm.displayText.value)
    }

    @Test
    fun `initial morseDone is false`() {
        assertFalse(vm.morseDone.value)
    }

    @Test
    fun `setWpm updates wpm`() {
        vm.setWpm(25)
        assertEquals(25, vm.wpm.value)
    }

    @Test
    fun `setMode changes mode when idle`() {
        vm.setMode(Mode.Learn)
        assertEquals(Mode.Learn, vm.mode.value)
    }

    @Test
    fun `setMode rejected when sending`() = runTest(StandardTestDispatcher()) {
        // Manually set state to Sending to simulate active playback
        val field = MorseViewModel::class.java.getDeclaredField("_appState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<AppState>
        flow.value = AppState.Sending

        vm.setMode(Mode.Learn)
        assertEquals(Mode.Test, vm.mode.value) // unchanged
    }

    @Test
    fun `onReveal resets to Idle`() {
        // Simulate a reveal state by setting article via reflection
        val articleField = MorseViewModel::class.java.getDeclaredField("article")
        articleField.isAccessible = true
        articleField.set(
            vm,
            com.michaelmorrow.morsetrainer.model.ArticleModel(
                title = "Test",
                sentence = "A test sentence.",
                url = "https://example.com",
            ),
        )
        val stateField = MorseViewModel::class.java.getDeclaredField("_appState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<AppState>).value = AppState.Reveal

        vm.onReveal()

        assertEquals(AppState.Idle, vm.appState.value)
        assert(vm.displayText.value.startsWith("Title:"))
        assert(vm.displayText.value.contains("Sentence:"))
        assert(vm.displayText.value.contains("Source:"))
    }
}
