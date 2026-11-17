package com.rizkysiregar.narrativ.domain.post

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.media.CompressedImage
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.testing.fakes.FakeImageCompressor
import com.rizkysiregar.narrativ.testing.fakes.FakeVideoCompressor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile

private const val MB = 1024L * 1024

/** How a picked media becomes a local file for the upload: copied, compressed, or refused. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FileMediaPreparationUseCaseTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val images = FakeImageCompressor()
	private val videos = FakeVideoCompressor()
	private lateinit var useCase: FileMediaPreparationUseCase
	private var uris = 0

	@Before
	fun setUp() {
		useCase = FileMediaPreparationUseCase(context, videos, images)
	}

	/** A content uri whose stream holds [size] bytes. */
	private fun picked(size: Int = 1_000): Uri {
		val uri = Uri.parse("content://picker/${uris++}")
		shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(ByteArray(size) { 7 }))
		return uri
	}

	private fun cacheFiles(): List<File> = context.cacheDir.listFiles().orEmpty().filter { it.name.startsWith("feed_upload_") }

	private fun file(name: String, size: Long = 100) = File(context.cacheDir, name).also {
		RandomAccessFile(it, "rw").use { f -> f.setLength(size) }
	}

	private suspend fun refusal(block: suspend () -> Unit): Int =
		(runCatching { block() }.exceptionOrNull() as MediaValidationException).messageRes

	// --- images ---

	@Test
	fun anImageIsCopiedToTheCacheAndTheCompressedFileIsTheOneToUpload() = runTest {
		val compressed = file("small.jpg")
		images.onCompress = { _, _ -> CompressedImage(compressed, "image/jpeg") }

		val result = useCase.prepare(picked(), "image/png")

		assertThat(result.filePath).isEqualTo(compressed.absolutePath)
		assertThat(result.contentType).isEqualTo("image/jpeg") // the one of the compressed file, it is what gets signed
		assertThat(images.compressed.single().readBytes()).hasLength(1_000) // the compressor got the full copy
	}

	@Test
	fun theCopyKeepsTheExtensionOfItsType() = runTest {
		listOf("image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp").forEach { (mime, extension) ->
			useCase.prepare(picked(), mime)

			assertThat(images.compressed.last().name).endsWith(".$extension")
		}
	}

	@Test
	fun aGifIsSentAsIsBecauseCompressingWouldDropTheAnimation() = runTest {
		val result = useCase.prepare(picked(size = 2 * MB.toInt()), "image/gif")

		assertThat(images.compressed).isEmpty()
		assertThat(result.contentType).isEqualTo("image/gif")
		assertThat(result.filePath).endsWith(".gif")
		assertThat(File(result.filePath).length()).isEqualTo(2 * MB)
	}

	@Test
	fun aGifOverFiveMegabytesIsRefusedAndItsCopyRemoved() = runTest {
		val error = refusal { useCase.prepare(picked(size = (5 * MB + 1).toInt()), "image/gif") }

		assertThat(error).isEqualTo(R.string.media_err_gif_size)
		assertThat(cacheFiles()).isEmpty()
	}

	@Test
	fun anUnsupportedImageFormatIsRefusedBeforeAnythingIsCopied() = runTest {
		val error = refusal { useCase.prepare(picked(), "image/bmp") }

		assertThat(error).isEqualTo(R.string.media_err_image_format)
		assertThat(cacheFiles()).isEmpty()
	}

	@Test
	fun aMediaWithoutAnyTypeIsRefused() = runTest {
		val error = refusal { useCase.prepare(picked(), null) }

		assertThat(error).isEqualTo(R.string.media_err_unknown_type)
	}

	// --- videos ---

	@Test
	fun everySupportedVideoTypeComesOutAsMp4() = runTest {
		videos.onCompress = { file("out.mp4", 1 * MB) }

		listOf("video/mp4", "video/quicktime", "video/webm").forEach { mime ->
			val result = useCase.prepare(picked(), mime)

			assertThat(result.contentType).isEqualTo("video/mp4")
			assertThat(result.filePath).endsWith("out.mp4")
		}
		assertThat(videos.compressed).hasSize(3)
	}

	@Test
	fun aVideoStillOverFiftyMegabytesAfterCompressionIsRefusedAndRemoved() = runTest {
		val big = file("big.mp4", 50 * MB + 1)
		videos.onCompress = { big }

		val error = refusal { useCase.prepare(picked(), "video/mp4") }

		assertThat(error).isEqualTo(R.string.media_err_video_size)
		assertThat(big.exists()).isFalse()
	}

	@Test
	fun aVideoOfExactlyFiftyMegabytesIsAccepted() = runTest {
		videos.onCompress = { file("edge.mp4", 50 * MB) }

		val result = useCase.prepare(picked(), "video/mp4")

		assertThat(File(result.filePath).exists()).isTrue()
	}

	@Test
	fun aVideoThatCannotBeProcessedKeepsItsReason() = runTest {
		videos.onCompress = { throw MediaValidationException(R.string.media_err_duration_max) }

		val error = refusal { useCase.prepare(picked(), "video/mp4") }

		assertThat(error).isEqualTo(R.string.media_err_duration_max)
	}

	@Test
	fun anythingElseIsAnUnsupportedFormat() = runTest {
		val error = refusal { useCase.prepare(picked(), "audio/mpeg") }

		assertThat(error).isEqualTo(R.string.media_err_format)
	}

	// --- batches ---

	@Test
	fun aBatchIsPreparedInOrder() = runTest {
		val first = file("one.jpg")
		val second = file("two.mp4", 1 * MB)
		images.onCompress = { _, _ -> CompressedImage(first, "image/jpeg") }
		videos.onCompress = { second }

		val result = useCase.prepareAll(listOf(PickedMedia(picked(), "image/jpeg"), PickedMedia(picked(), "video/mp4")))

		assertThat(result.map { it.filePath }).containsExactly(first.absolutePath, second.absolutePath).inOrder()
		assertThat(result.map { it.contentType }).containsExactly("image/jpeg", "video/mp4").inOrder()
	}

	@Test
	fun theBatchIsMarkedAsPreparingFromTheFirstMediaToTheLast() = runTest {
		val seen = mutableListOf<Boolean>()
		images.onCompress = { source, mime -> seen += useCase.isPreparing.value; CompressedImage(source, mime) }
		assertThat(useCase.isPreparing.value).isFalse()

		useCase.prepareAll(listOf(PickedMedia(picked(), "image/jpeg"), PickedMedia(picked(), "image/jpeg")))

		assertThat(seen).containsExactly(true, true)
		assertThat(useCase.isPreparing.value).isFalse()
	}

	@Test
	fun whenOneMediaIsRefusedTheOnesAlreadyPreparedAreDeleted() = runTest {
		var n = 0
		images.onCompress = { _, _ -> CompressedImage(file("prepared_${n++}.jpg"), "image/jpeg") }
		val batch = listOf(PickedMedia(picked(), "image/jpeg"), PickedMedia(picked(), "image/jpeg"), PickedMedia(picked(), "image/bmp"))

		val error = refusal { useCase.prepareAll(batch) }

		assertThat(error).isEqualTo(R.string.media_err_image_format)
		assertThat(File(context.cacheDir, "prepared_0.jpg").exists()).isFalse()
		assertThat(File(context.cacheDir, "prepared_1.jpg").exists()).isFalse()
		assertThat(useCase.isPreparing.value).isFalse()
	}

	@Test
	fun anEmptyBatchPreparesNothing() = runTest {
		assertThat(useCase.prepareAll(emptyList())).isEmpty()
		assertThat(useCase.isPreparing.value).isFalse()
	}
}
