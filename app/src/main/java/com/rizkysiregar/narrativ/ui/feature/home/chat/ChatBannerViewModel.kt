package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.chat.ChatAvatars
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.chat.ChatSessionState
import com.rizkysiregar.narrativ.config.FeatureFlags
import com.rizkysiregar.narrativ.notification.ChatSound
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shows a banner for a chat message that arrives while the app is open (the server sends no push then: the app is
 * connected) in a conversation that is not on screen, with Narrativ's notification sound. One banner at a time, the
 * newest wins; it goes by itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatBannerViewModel @Inject constructor(
	private val chat: ChatSession,
	featureFlags: FeatureFlags,
	private val avatars: ChatAvatars,
	private val sound: ChatSound,
) : ViewModel() {
	private val _banner = MutableStateFlow<ChatBanner?>(null)
	val banner: StateFlow<ChatBanner?> = _banner.asStateFlow()
	private var hide: Job? = null

	init {
		viewModelScope.launch {
			combine(featureFlags.chatEnabled, chat.state) { enabled, session -> enabled to session }
				.flatMapLatest { (enabled, session) ->
					if (enabled && session is ChatSessionState.Ready) session.client.incoming else emptyFlow()
				}
				.collect { e -> chatBannerOf(e, chat.visibleChannelId)?.let(::show) }
		}
	}

	private fun show(b: ChatBanner) {
		_banner.value = b
		sound.play()
		// a picture uploaded to Narrativ is not in the message: shown as soon as the profile answers
		if (b.avatarUrl == null) viewModelScope.launch {
			val url = avatars.of(b.senderId) ?: return@launch
			_banner.update { if (it?.messageId == b.messageId) it.copy(avatarUrl = url) else it }
		}
		hide?.cancel()
		hide = viewModelScope.launch {
			delay(SHOWN_MS)
			if (_banner.value?.messageId == b.messageId) _banner.value = null
		}
	}

	fun dismiss() {
		hide?.cancel()
		_banner.value = null
	}

	private companion object {
		const val SHOWN_MS = 4_500L
	}
}
