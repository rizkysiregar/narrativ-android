package com.rizkysiregar.narrativ.testing.fakes

import android.net.Uri
import com.rizkysiregar.narrativ.chat.ChatAttachmentReader
import com.rizkysiregar.narrativ.chat.PickedFile

class FakeChatAttachmentReader : ChatAttachmentReader {
	var file: PickedFile? = PickedFile(ByteArray(12), "image/png", "dot.png")
	override suspend fun read(uri: Uri): PickedFile? = file
}
