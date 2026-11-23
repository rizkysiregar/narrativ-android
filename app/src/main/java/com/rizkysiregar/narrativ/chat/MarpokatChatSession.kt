package com.rizkysiregar.narrativ.chat

import android.util.Log
import com.rizkysiregar.marpokat.TokenProvider
import com.rizkysiregar.narrativ.data.remote.api.ChatApi
import com.rizkysiregar.narrativ.data.remote.model.response.ChatTokenResponse
import com.rizkysiregar.narrativ.data.session.PushTokenProvider
import com.rizkysiregar.narrativ.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarpokatChatSession @Inject constructor(
	retrofit: Retrofit,
	private val factory: ChatClientFactory,
	private val pushTokens: PushTokenProvider,
	@ApplicationScope private val scope: CoroutineScope
) : ChatSession {
	private val api: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
	private val _state = MutableStateFlow<ChatSessionState>(ChatSessionState.Idle)
	override val state: StateFlow<ChatSessionState> = _state.asStateFlow()
	override var visibleChannelId: String? = null

	private val lock = Mutex()
	private var connection: ChatConnection? = null

	override fun start() {
		if (_state.value is ChatSessionState.Starting || _state.value is ChatSessionState.Ready) return
		_state.value = ChatSessionState.Starting
		scope.launch {
			lock.withLock {
				if (connection != null) return@withLock
				try {
					val credentials = fetchCredentials()
					var firstToken: String? = credentials.token
					val created = factory.create(credentials) {
						// the token fetched to learn the address serves the first connection; later ones ask again
						firstToken?.also { firstToken = null } ?: fetchCredentials().token
					}
					connection = created
					_state.value = ChatSessionState.Ready(created.client, credentials.userId)
					registerPushToken()
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					Log.w(TAG, "Chat is not available", e)
					_state.value = ChatSessionState.Unavailable
				}
			}
		}
	}

	override suspend fun openDirectMessage(userId: String): String = try {
		api.openDirectMessage(userId).data?.channelId ?: throw ChatException("The direct message answer has no channel")
	} catch (e: CancellationException) {
		throw e
	} catch (e: HttpException) {
		throw ChatException("Chat refused (${e.code()})", notFound = e.code() == 404, cause = e)
	} catch (e: IOException) {
		throw ChatException("Chat could not be reached", cause = e)
	}

	override fun onPushToken(token: String) {
		val ready = _state.value as? ChatSessionState.Ready ?: return // registered when the session starts
		scope.launch { runCatching { ready.client.registerPushToken(token) } }
	}

	override suspend fun signOut() {
		lock.withLock {
			val current = connection ?: return@withLock run { _state.value = ChatSessionState.Idle }
			connection = null
			current.stop()
			// the token may already be gone, or the network too: signing out must never fail because of chat
			runCatching { current.client.unregisterPushToken(pushTokens.token()) }
			runCatching { current.client.reset() }
			current.client.close()
			_state.value = ChatSessionState.Idle
		}
	}

	private suspend fun registerPushToken() {
		val ready = _state.value as? ChatSessionState.Ready ?: return
		try {
			ready.client.registerPushToken(pushTokens.token())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.w(TAG, "Chat push token not registered", e) // chat works without it; the token is sent again when it rotates
		}
	}

	private suspend fun fetchCredentials(): ChatTokenResponse =
		api.token().data ?: throw ChatException("The chat token answer is empty")

	private companion object {
		const val TAG = "ChatSession"
	}
}
