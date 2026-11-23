package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.marpokat.HttpReply
import com.rizkysiregar.marpokat.HttpTransport
import com.rizkysiregar.marpokat.MarpokatClient
import com.rizkysiregar.marpokat.MarpokatOptions
import com.rizkysiregar.marpokat.Socket
import com.rizkysiregar.marpokat.SocketFactory
import com.rizkysiregar.marpokat.SocketListener
import kotlinx.coroutines.CoroutineDispatcher
import java.net.URI

const val CHAT_CHANNEL = "ch-1"
const val ME = """{"id":"x1","external_id":"u-me","name":"Me","deleted":false}"""
const val BUDI = """{"id":"x2","external_id":"u-budi","name":"Budi","deleted":false}"""

fun chatMessage(seq: Int, text: String = "t$seq", sender: String = BUDI) =
	"""{"id":"m$seq","channel_id":"$CHAT_CHANNEL","seq":$seq,"rev_seq":$seq,"type":"text","sender":$sender,"text":"$text","attachments":[],"reactions":{"counts":{},"mine":[]},"created_at":"2026-11-22T10:00:0${seq}Z"}"""

fun chatChannel(unread: Int = 0, frozen: Boolean = false) =
	"""{"id":"$CHAT_CHANNEL","type":"dm","member_count":2,"dm_peer":$BUDI,"last_seq":2,"last_message_seq":2,"frozen":$frozen,"created_at":"2026-01-01T00:00:00Z","membership":{"role":"member","unread_count":$unread,"last_read_seq":0},"last_message":${chatMessage(2)},"last_message_at":"2026-11-22T10:00:02Z"}"""

/** A scripted Marpokat server for the SDK client: REST answers and a socket the test pushes frames through. */
class FakeChatBackend : HttpTransport, SocketFactory {
	val calls = mutableListOf<String>()
	val bodies = mutableListOf<String>()
	val puts = mutableListOf<String>()
	var channels = listOf(chatChannel())
	var history = listOf(chatMessage(1), chatMessage(2))
	var failMessagesWith: Int? = null
	var socket: Sent? = null

	class Sent(val listener: SocketListener) : Socket {
		val frames = mutableListOf<String>()
		override fun send(text: String): Boolean { frames += text; return true }
		override fun close(code: Int) {}
		fun push(type: String, data: String) = listener.onText("""{"type":"$type","data":$data}""")
	}

	override fun open(url: String, listener: SocketListener): Socket = Sent(listener).also { socket = it; listener.onOpen() }

	override suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpReply {
		val uri = URI(url)
		val key = "$method ${uri.rawPath}"
		calls += key
		bodies += body.orEmpty()
		return when {
			key == "GET /v1/channels" -> HttpReply(200, """{"channels":[${channels.joinToString(",")}],"next_cursor":null}""")
			key == "GET /v1/presence" -> HttpReply(200, """{"users":[{"user_id":"u-budi","online":true}]}""")
			key == "GET /v1/channels/$CHAT_CHANNEL/messages" ->
				failMessagesWith?.let { HttpReply(it, """{"error":{"code":"boom","message":"boom"}}""") }
					?: HttpReply(200, """{"messages":[${history.reversed().joinToString(",")}],"has_more":false}""")
			key == "POST /v1/attachments" -> HttpReply(201, """{"attachment":{"id":"att1","kind":"image"},"upload_url":"https://files.test/put","upload_headers":{"Content-Type":"image/png"},"expires_at":""}""")
			key == "POST /v1/attachments/att1/complete" -> HttpReply(200, """{"id":"att1","kind":"image","status":"processing"}""")
			key == "POST /v1/devices" || key.startsWith("DELETE /v1/devices/") -> HttpReply(204, "")
			key.startsWith("PUT /v1/messages/") && key.contains("/reactions/") ->
				HttpReply(200, """{"id":"m2","channel_id":"$CHAT_CHANNEL","seq":2,"rev_seq":3,"type":"text","sender":$BUDI,"text":"t2","reactions":{"counts":{"👍":1},"mine":["👍"]},"created_at":"2026-11-22T10:00:02Z"}""")
			key == "PATCH /v1/messages/m3" -> HttpReply(200, """{"id":"m3","channel_id":"$CHAT_CHANNEL","seq":3,"rev_seq":4,"type":"text","sender":$ME,"text":"edited","edited_at":"2026-11-22T10:01:00Z","reactions":{"counts":{},"mine":[]},"created_at":"2026-11-22T10:00:03Z"}""")
			key.startsWith("DELETE /v1/messages/") -> HttpReply(204, "")
			else -> HttpReply(404, """{"error":{"code":"not_found","message":"$key"}}""")
		}
	}

	override suspend fun put(url: String, headers: Map<String, String>, body: ByteArray, onProgress: (Long, Long) -> Unit): Int {
		puts += url
		onProgress(body.size.toLong(), body.size.toLong())
		return 200
	}

	/** A client wired to this backend. [dispatcher] is the test's, so virtual time and `runCurrent` apply. */
	fun client(dispatcher: CoroutineDispatcher) = MarpokatClient(
		MarpokatOptions(
			baseUrl = "https://chat.test", apiKey = "mpk_t", tokenProvider = { "tok" }, http = this, sockets = this,
			dispatcher = dispatcher, backoffBaseMs = 10, random = { 1.0 }
		)
	)

	/** Connects [client] and answers the auth frame, as the server would. */
	suspend fun connect(client: MarpokatClient) {
		client.connect()
		var waited = 0
		while (socket == null && waited++ < 200) kotlinx.coroutines.yield()
		socket!!.push("auth.ok", """{"user":$ME,"heartbeat_interval_ms":25000}""")
	}
}
