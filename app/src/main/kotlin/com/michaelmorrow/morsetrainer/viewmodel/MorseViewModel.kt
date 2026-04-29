package com.michaelmorrow.morsetrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelmorrow.morsetrainer.model.ArticleModel
import com.michaelmorrow.morsetrainer.service.MorseEngine
import com.michaelmorrow.morsetrainer.service.WikipediaService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppState { Idle, Loading, Sending, Reveal }
enum class Mode { Learn, Test }

class MorseViewModel : ViewModel() {

    private val _appState = MutableStateFlow(AppState.Idle)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _morseDone = MutableStateFlow(false)
    val morseDone: StateFlow<Boolean> = _morseDone.asStateFlow()

    private val _mode = MutableStateFlow(Mode.Test)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _wpm = MutableStateFlow(30)
    val wpm: StateFlow<Int> = _wpm.asStateFlow()

    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _articleUrl = MutableStateFlow<String?>(null)
    val articleUrl: StateFlow<String?> = _articleUrl.asStateFlow()

    private var article: ArticleModel? = null
    private var playbackJob: Job? = null
    private val morseEngine = MorseEngine()

    fun onFindArticle() {
        if (_appState.value != AppState.Idle) return
        _appState.value = AppState.Loading
        _displayText.value = ""
        _articleUrl.value = null
        _morseDone.value = false

        viewModelScope.launch {
            try {
                val fetched = WikipediaService.fetchRandomArticle()
                article = fetched
                startPlayback(fetched)
            } catch (e: Exception) {
                _displayText.value = "Error fetching article: ${e.message}"
                _appState.value = AppState.Idle
            }
        }
    }

    private fun startPlayback(fetched: ArticleModel) {
        _appState.value = AppState.Sending
        _displayText.value = if (_mode.value == Mode.Test) "Sending …" else ""

        playbackJob = viewModelScope.launch {
            morseEngine.play(
                sentence = fetched.sentence,
                getCpm = { _wpm.value * 5 },
                onCharacterStart = { char ->
                    if (_mode.value == Mode.Learn) {
                        _displayText.value = _displayText.value + char
                    }
                },
            )
            // Natural completion (not cancelled)
            if (_appState.value == AppState.Sending) {
                if (_mode.value == Mode.Test) {
                    _displayText.value = "Send complete…"
                }
                _appState.value = AppState.Reveal
                _morseDone.value = true
            }
        }
    }

    fun onStopSending() {
        playbackJob?.cancel()
        playbackJob = null
        _displayText.value = "Stopped …"
        _appState.value = AppState.Reveal
        _morseDone.value = true
    }

    fun onReveal() {
        val art = article ?: return
        _displayText.value = "Title: ${art.title}\nSentence: ${art.sentence}\nSource: ${art.url}"
        _articleUrl.value = art.url
        _appState.value = AppState.Idle
    }

    fun setWpm(value: Int) {
        _wpm.value = value
    }

    fun setMode(value: Mode) {
        if (_appState.value == AppState.Sending || _appState.value == AppState.Loading) return
        _mode.value = value
    }
}
