package com.rizkysiregar.narrativ.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A file the user picked, read into memory (chat attachments are at most 10 MB for a picture, 50 MB for a video). */
class PickedFile(val bytes: ByteArray, val mime: String, val name: String?)

/** Reads what the user picked. Behind an interface so view model tests need no content resolver. */
interface ChatAttachmentReader {
	/** Null when the file cannot be read (the app that shared it is gone, permission lost). */
	suspend fun read(uri: Uri): PickedFile?
}

@Singleton
class ContentResolverAttachmentReader @Inject constructor(
	@ApplicationContext private val context: Context
) : ChatAttachmentReader {
	override suspend fun read(uri: Uri): PickedFile? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
		runCatching {
			val resolver = context.contentResolver
			val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
			val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
				if (c.moveToFirst()) c.getString(0) else null
			}
			PickedFile(bytes, resolver.getType(uri) ?: "application/octet-stream", name)
		}.getOrNull()
	}
}
