package com.rizkysiregar.narrativ.chat

import com.rizkysiregar.narrativ.data.remote.api.UserApi
import com.rizkysiregar.narrativ.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The profile pictures of chat members, by Narrativ user id (the Marpokat external id).
 *
 * Marpokat only knows pictures that never expire (a Google picture). A picture uploaded to Narrativ is reachable only
 * through a presigned URL that expires within an hour, so the backend does not give it to Marpokat: it is asked from
 * the Narrativ profile here, and asked again before it expires. A person without a picture, or one that cannot be
 * looked up, keeps the generated avatar.
 */
interface ChatAvatars {
	/** Narrativ user id → picture URL, for the people that have one and were asked for. */
	val urls: StateFlow<Map<String, String>>

	/** Looks these people up in the background, when not known or about to expire. */
	fun want(userIds: Collection<String>)

	/** The picture of one person, looked up when needed; null when there is none. */
	suspend fun of(userId: String): String?
}

@Singleton
class NarrativChatAvatars internal constructor(
	private val fetch: suspend (String) -> String?,
	private val scope: CoroutineScope,
	private val now: () -> Long,
) : ChatAvatars {

	@Inject
	constructor(retrofit: Retrofit, @ApplicationScope scope: CoroutineScope) : this(
		fetch = { id ->
			retrofit.create(UserApi::class.java).getUserProfile(id).data?.profileImageUrl?.takeIf { it.isNotBlank() }
		},
		scope = scope,
		now = System::currentTimeMillis,
	)

	private class Known(val url: String?, val until: Long)

	private val lock = Any()
	private val known = HashMap<String, Known>()
	private val loading = HashMap<String, Deferred<String?>>()
	private val _urls = MutableStateFlow<Map<String, String>>(emptyMap())
	override val urls: StateFlow<Map<String, String>> = _urls.asStateFlow()

	override fun want(userIds: Collection<String>) {
		userIds.filter { it.isNotBlank() && !isFresh(it) }.distinct().forEach { id -> scope.launch { of(id) } }
	}

	override suspend fun of(userId: String): String? {
		if (userId.isBlank()) return null
		val pending = synchronized(lock) {
			known[userId]?.takeIf { it.until > now() }?.let { return it.url }
			loading.getOrPut(userId) { scope.async { load(userId) } }
		}
		return pending.await()
	}

	private fun isFresh(id: String) = synchronized(lock) { (known[id]?.until ?: 0) > now() || id in loading }

	private suspend fun load(id: String): String? {
		val (url, keepFor) = try {
			fetch(id) to FRESH_MS
		} catch (e: CancellationException) {
			throw e
		} catch (e: HttpException) {
			// gone, blocked, not allowed: not asked again soon; a server error is
			null to if (e.code() in 400..499) FRESH_MS else RETRY_MS
		} catch (e: Exception) {
			null to RETRY_MS // offline: the generated avatar until it can be asked again
		}
		synchronized(lock) {
			known[id] = Known(url, now() + keepFor)
			loading.remove(id)
		}
		_urls.update { if (url != null) it + (id to url) else it - id }
		return url
	}

	internal companion object {
		/** Presigned URLs last an hour: asked again well before. */
		const val FRESH_MS = 40 * 60_000L
		const val RETRY_MS = 2 * 60_000L
	}
}
