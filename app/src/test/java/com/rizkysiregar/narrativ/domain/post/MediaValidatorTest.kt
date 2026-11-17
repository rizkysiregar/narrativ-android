package com.rizkysiregar.narrativ.domain.post

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.testing.fakes.FakeMediaInspector
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val MB = 1024L * 1024

// Robolectric only to create a Uri; the validator itself reads everything through the inspector.
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MediaValidatorTest {

	private val inspector = FakeMediaInspector()
	private val validator = MediaValidator(inspector)
	private val uri: Uri = Uri.parse("content://media/1")

	private fun check(mime: String?, size: Long? = null, duration: Long? = null) =
		validator.check(uri.also { inspector.describe(it, mime, size, duration) })

	@Test
	fun theTypeIsReturnedWithTheVerdict() {
		assertThat(check("image/png").mimeType).isEqualTo("image/png")
	}

	@Test
	fun aMediaWithoutATypeIsRefused() {
		assertThat(check(null).errorRes).isEqualTo(R.string.media_err_unknown_type)
	}

	@Test
	fun staticImagesAreAcceptedWhateverTheirSizeBecauseTheyAreCompressed() {
		listOf("image/jpeg", "image/png", "image/webp").forEach {
			assertThat(check(it, size = 40 * MB).errorRes).isNull()
		}
	}

	@Test
	fun anUnsupportedImageFormatIsRefused() {
		assertThat(check("image/bmp").errorRes).isEqualTo(R.string.media_err_image_format)
		assertThat(check("image/heic").errorRes).isEqualTo(R.string.media_err_image_format)
	}

	@Test
	fun aGifIsLimitedTo5MBBecauseItIsSentAsIs() {
		assertThat(check("image/gif", size = 5 * MB).errorRes).isNull()
		assertThat(check("image/gif", size = 5 * MB + 1).errorRes).isEqualTo(R.string.media_err_gif_size)
	}

	@Test
	fun aGifWhoseSizeCannotBeReadIsLetThrough() {
		assertThat(check("image/gif", size = null).errorRes).isNull()
	}

	@Test
	fun theSupportedVideoTypesAreAccepted() {
		listOf("video/mp4", "video/quicktime", "video/webm").forEach {
			assertThat(check(it, size = 10 * MB, duration = 30_000).errorRes).isNull()
		}
	}

	@Test
	fun aVideoIsLimitedTo50MBAndSixtySecondsInclusive() {
		assertThat(check("video/mp4", size = 50 * MB, duration = 60_000).errorRes).isNull()
		assertThat(check("video/mp4", size = 50 * MB + 1, duration = 1_000).errorRes).isEqualTo(R.string.media_err_video_50)
		assertThat(check("video/mp4", size = 1 * MB, duration = 60_001).errorRes).isEqualTo(R.string.media_err_duration_max)
	}

	@Test
	fun aVideoThatIsTooBigIsRefusedForSizeBeforeDuration() {
		assertThat(check("video/mp4", size = 60 * MB, duration = 120_000).errorRes).isEqualTo(R.string.media_err_video_50)
	}

	@Test
	fun aVideoWhoseSizeOrDurationCannotBeReadIsLetThrough() {
		// the preparation step still checks the real file before it is uploaded
		assertThat(check("video/mp4", size = null, duration = null).errorRes).isNull()
	}

	@Test
	fun anythingElseIsAnUnsupportedFormat() {
		assertThat(check("audio/mpeg").errorRes).isEqualTo(R.string.media_err_format)
		assertThat(check("application/pdf").errorRes).isEqualTo(R.string.media_err_format)
		assertThat(check("video/x-matroska").errorRes).isEqualTo(R.string.media_err_format)
	}
}
