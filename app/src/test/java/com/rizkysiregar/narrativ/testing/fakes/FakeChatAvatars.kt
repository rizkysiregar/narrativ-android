package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.chat.ChatAvatars
import com.rizkysiregar.narrativ.notification.ChatSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Pictures the test chooses in [pictures]; [want] makes the wanted ones known, as the real lookup would. */
class FakeChatAvatars(var pictures: Map<String, String> = emptyMap()) : ChatAvatars {
	private val _urls = MutableStateFlow<Map<String, String>>(emptyMap())
	override val urls: StateFlow<Map<String, String>> = _urls
	val wanted = mutableListOf<String>()

	override fun want(userIds: Collection<String>) {
		wanted += userIds
		val found = userIds.mapNotNull { id -> pictures[id]?.let { id to it } }.toMap()
		if (!_urls.value.entries.containsAll(found.entries)) _urls.value = _urls.value + found
	}

	override suspend fun of(userId: String): String? = pictures[userId]
}

class FakeChatSound : ChatSound {
	var plays = 0
	override fun play() { plays++ }
}
