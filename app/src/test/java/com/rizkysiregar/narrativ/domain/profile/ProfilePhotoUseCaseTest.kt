package com.rizkysiregar.narrativ.domain.profile

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.remote.upload.PresignedUploader
import com.rizkysiregar.narrativ.data.remote.upload.UploadHttpException
import com.rizkysiregar.narrativ.media.CompressedImage
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.testing.fakes.FakeImageCompressor
import com.rizkysiregar.narrativ.testing.fakes.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ProfilePhotoUseCaseTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val users = FakeUserRepository()
	private val images = FakeImageCompressor()
	private val me = UserSearchItemResponse(id = "u1", username = "rizky", profileImageUrl = "https://cdn/avatar.jpg")
	private lateinit var server: MockWebServer
	private lateinit var useCase: ProfilePhotoUseCase
	private var uris = 0

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
		server.enqueue(MockResponse.Builder().code(200).build())
		useCase = ProfilePhotoUseCase(context, users, images, PresignedUploader(OkHttpClient(), Dispatchers.IO))
		users.onCreateProfileImageUploadUrl = { ProfileImageUploadUrlResponse(server.url("/avatars/1").toString(), "avatars/1.jpg", 900) }
		users.onSetProfileImage = { me }
	}

	@After
	fun tearDown() = server.close()

	private fun picked(bytes: Int = 500): Uri {
		val uri = Uri.parse("content://picker/avatar/${uris++}")
		shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(ByteArray(bytes) { 3 }))
		return uri
	}

	private fun cached(name: String, text: String = "avatar bytes") = File(context.cacheDir, name).apply { writeText(text) }

	private fun upload(uri: Uri = picked(), mime: String? = "image/png", onProgress: (Float) -> Unit = {}) =
		runBlocking { useCase.upload(uri, mime, onProgress) }

	@Test
	fun aPickedPhotoIsShrunkUploadedToStorageAndConfirmedToTheBackend() {
		val shrunk = cached("shrunk.jpg", "small avatar")
		images.onCompressAvatar = { _, _ -> CompressedImage(shrunk, "image/jpeg") }
		val progress = mutableListOf<Float>()

		val result = upload(mime = "image/png", onProgress = { progress += it })

		assertThat(result).isEqualTo(me)
		assertThat(images.avatarsCompressed.single().name).startsWith("avatar_upload_")
		assertThat(users.profileImageUploadUrlRequests).containsExactly("image/jpeg") // the type of what is uploaded
		val put = server.takeRequest()
		assertThat(put.method).isEqualTo("PUT")
		assertThat(put.url.encodedPath).isEqualTo("/avatars/1")
		assertThat(put.headers["Content-Type"]).isEqualTo("image/jpeg")
		assertThat(put.body!!.utf8()).isEqualTo("small avatar")
		assertThat(users.profileImageKeys).containsExactly("avatars/1.jpg")
		assertThat(progress.last()).isEqualTo(1f)
	}

	@Test
	fun theShrunkFileIsDeletedAfterwards() {
		val shrunk = cached("shrunk.jpg")
		images.onCompressAvatar = { _, _ -> CompressedImage(shrunk, "image/jpeg") }

		upload()

		assertThat(shrunk.exists()).isFalse()
	}

	@Test
	fun onlyJpegPngAndWebpArePhotos() {
		listOf("image/gif", "image/bmp", "video/mp4", null).forEach { mime ->
			val error = runCatching { upload(mime = mime) }.exceptionOrNull() as MediaValidationException

			assertThat(error.messageRes).isEqualTo(R.string.media_err_photo_format)
		}
		assertThat(images.avatarsCompressed).isEmpty()
		assertThat(users.profileImageUploadUrlRequests).isEmpty()
	}

	@Test
	fun anImageThatCannotBeReadStopsBeforeAnyRequest() {
		images.onCompressAvatar = { _, _ -> throw MediaValidationException(R.string.media_err_image_read) }

		val error = runCatching { upload() }.exceptionOrNull() as MediaValidationException

		assertThat(error.messageRes).isEqualTo(R.string.media_err_image_read)
		assertThat(users.profileImageUploadUrlRequests).isEmpty()
		assertThat(server.requestCount).isEqualTo(0)
	}

	@Test
	fun aFailedStorageUploadIsNotConfirmedAndStillCleansUp() {
		val shrunk = cached("shrunk.jpg")
		images.onCompressAvatar = { _, _ -> CompressedImage(shrunk, "image/jpeg") }
		server.dispatcher = object : mockwebserver3.Dispatcher() {
			override fun dispatch(request: mockwebserver3.RecordedRequest) = MockResponse.Builder().code(403).build()
		}

		val error = runCatching { upload() }.exceptionOrNull()

		assertThat(error).isInstanceOf(UploadHttpException::class.java)
		assertThat((error as UploadHttpException).code).isEqualTo(403)
		assertThat(users.profileImageKeys).isEmpty() // the backend is never told about a photo that is not there
		assertThat(shrunk.exists()).isFalse()
	}

	@Test
	fun removingThePhotoAsksTheBackend() {
		users.onDeleteProfileImage = { me.copy(profileImageUrl = null) }

		val result = runBlocking { useCase.remove() }

		assertThat(result.profileImageUrl).isNull()
		assertThat(users.profileImageDeletions).isEqualTo(1)
	}
}
