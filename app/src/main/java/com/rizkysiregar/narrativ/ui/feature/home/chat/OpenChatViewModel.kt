package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.chat.ChatException
import com.rizkysiregar.narrativ.chat.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OpenChatState {
	data object Idle : OpenChatState
	data object Opening : OpenChatState

	/** The channel to show. The screen takes it and calls [OpenChatViewModel.consumed]. */
	data class Opened(val channelId: String) : OpenChatState

	/** [notFound]: the backend says this person cannot be messaged (blocked, gone). */
	data class Failed(val notFound: Boolean) : OpenChatState
}

/** The Message button on a profile: asks the backend for the direct message with that user. */
@HiltViewModel
class OpenChatViewModel @Inject constructor(
	private val chat: ChatSession
) : ViewModel() {
	private val _state = MutableStateFlow<OpenChatState>(OpenChatState.Idle)
	val state: StateFlow<OpenChatState> = _state.asStateFlow()

	fun open(userId: String) {
		if (_state.value == OpenChatState.Opening) return
		_state.value = OpenChatState.Opening
		viewModelScope.launch {
			_state.value = try {
				OpenChatState.Opened(chat.openDirectMessage(userId))
			} catch (e: CancellationException) {
				throw e
			} catch (e: ChatException) {
				OpenChatState.Failed(e.notFound)
			}
		}
	}

	fun consumed() {
		_state.value = OpenChatState.Idle
	}
}
