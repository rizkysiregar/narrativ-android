package com.rizkysiregar.feedzmore.data.remote.upload

import androidx.work.Data
import androidx.work.workDataOf
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The upload worker gets its files as string arrays. A caption-only post sends empty ones, which must
 * survive WorkManager persisting and reloading the request (otherwise that post would never be sent).
 */
class PostUploadInputDataTest {
	@Test
	fun captionOnlyPostKeepsItsEmptyFileListAfterReload() {
		val media = emptyList<String>()
		val data = workDataOf(
			PostUploadWorker.KEY_FILE_PATHS to media.toTypedArray(),
			PostUploadWorker.KEY_CONTENT_TYPES to media.toTypedArray(),
			PostUploadWorker.KEY_CAPTION to "hanya caption"
		)
		// what WorkManager does when it persists and reloads the request
		val reloaded = Data.fromByteArray(data.toByteArray())

		assertNotNull("empty file_paths came back null", reloaded.getStringArray(PostUploadWorker.KEY_FILE_PATHS))
		assertArrayEquals(emptyArray<String>(), reloaded.getStringArray(PostUploadWorker.KEY_FILE_PATHS))
		assertArrayEquals(emptyArray<String>(), reloaded.getStringArray(PostUploadWorker.KEY_CONTENT_TYPES))
		assertEquals("hanya caption", reloaded.getString(PostUploadWorker.KEY_CAPTION))
	}

	@Test
	fun severalMediaKeepTheirOrderAfterReload() {
		val data = workDataOf(
			PostUploadWorker.KEY_FILE_PATHS to arrayOf("/a.jpg", "/b.mp4"),
			PostUploadWorker.KEY_CONTENT_TYPES to arrayOf("image/jpeg", "video/mp4")
		)
		val reloaded = Data.fromByteArray(data.toByteArray())
		assertArrayEquals(arrayOf("/a.jpg", "/b.mp4"), reloaded.getStringArray(PostUploadWorker.KEY_FILE_PATHS))
		assertArrayEquals(arrayOf("image/jpeg", "video/mp4"), reloaded.getStringArray(PostUploadWorker.KEY_CONTENT_TYPES))
	}
}
