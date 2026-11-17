package com.rizkysiregar.narrativ.data.remote.contract

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.remote.api.AuthApi
import com.rizkysiregar.narrativ.data.remote.api.FeedApi
import com.rizkysiregar.narrativ.data.remote.api.PostApi
import com.rizkysiregar.narrativ.data.remote.api.UserApi
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.EmailRequest
import com.rizkysiregar.narrativ.data.remote.model.request.post.CreatePostUploadUrlRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.MediaType
import com.rizkysiregar.narrativ.util.flowFromApi
import com.rizkysiregar.narrativ.util.flowFromMessageApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

/**
 * The API interfaces, the Gson converter and the response models together, on the JSON in
 * `src/test/resources/fixtures` (see the README there: these are written from the models, not captured from the
 * backend). The Retrofit set-up is the one of NetworkModule.
 */
class ApiContractTest {

	private lateinit var server: MockWebServer
	private lateinit var retrofit: Retrofit

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
		retrofit = Retrofit.Builder()
			.baseUrl(server.url("/api/v1/"))
			.addConverterFactory(GsonConverterFactory.create())
			.build()
	}

	@After
	fun tearDown() = server.close()

	private fun fixture(name: String): String =
		checkNotNull(javaClass.classLoader!!.getResourceAsStream("fixtures/$name")) { "missing fixture $name" }
			.bufferedReader().use { it.readText() }

	private fun respond(name: String, code: Int = 200) {
		server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(fixture(name)).build())
	}

	@Test
	fun loginMapsEveryField() = runTest {
		respond("login_success.json")

		val data = retrofit.create(AuthApi::class.java).login(LoginRequest("rizky", "secret")).data!!

		assertThat(data.token).isEqualTo("access-1")
		assertThat(data.refreshToken).isEqualTo("refresh-1")
		assertThat(data.username).isEqualTo("rizky")
		assertThat(data.userId).isEqualTo("6f1d2c3a-0000-4000-8000-000000000001")
		assertThat(data.role).isEqualTo("USER")
		val request = server.takeRequest()
		assertThat(request.method).isEqualTo("POST")
		assertThat(request.url.encodedPath).isEqualTo("/api/v1/auth/login")
		assertThat(request.body!!.utf8()).contains("\"username\":\"rizky\"")
	}

	@Test
	fun aFeedPageMapsMediaPagingAndModeration() = runTest {
		respond("feed_page.json")

		val page = retrofit.create(FeedApi::class.java).getFeed(page = 0, size = 2).data!!

		assertThat(page.last).isFalse()
		assertThat(page.totalElements).isEqualTo(5)
		val (withMedia, takenDown) = page.content
		assertThat(withMedia.media!!.map { it.type }).containsExactly(MediaType.IMAGE, MediaType.VIDEO).inOrder()
		assertThat(withMedia.hasVideo).isTrue()
		assertThat(withMedia.videoAt(1)!!.thumbnailUrl).isEqualTo("https://cdn.example/m2.jpg")
		assertThat(withMedia.videoAt(0)).isNull()
		assertThat(withMedia.userId).isNotNull()
		assertThat(takenDown.isTakenDown).isTrue()
		assertThat(takenDown.takedownMessage).isEqualTo("Melanggar aturan komunitas")
		assertThat(takenDown.caption).isNull()
		val request = server.takeRequest()
		assertThat(request.url.encodedPath).isEqualTo("/api/v1/posts")
		assertThat(request.url.query).isEqualTo("page=0&size=2")
	}

	@Test
	fun aPostWithoutAMediaKeyHasNoMedia() = runTest {
		respond("feed_page.json")

		val takenDown = retrofit.create(FeedApi::class.java).getFeed(0, 2).data!!.content[1]

		assertThat(takenDown.media).isNull() // Gson leaves it null, which is why the model is read via mediaItems
		assertThat(takenDown.mediaItems).isEmpty()
		assertThat(takenDown.hasMedia).isFalse()
	}

	@Test
	fun exploreAsksForMediaOnlyWhenTheGridDoes() = runTest {
		respond("feed_page.json")

		retrofit.create(FeedApi::class.java).getExplorePosts(page = 1, size = 20, mediaOnly = true)

		val request = server.takeRequest()
		assertThat(request.url.encodedPath).isEqualTo("/api/v1/posts/explore")
		assertThat(request.url.query).isEqualTo("page=1&size=20&mediaOnly=true")
	}

	@Test
	fun aSinglePostWithEmptyMediaIsACaptionOnlyPost() = runTest {
		respond("post_single.json")

		val post = retrofit.create(FeedApi::class.java).getPost(UUID.fromString("11111111-1111-4111-8111-111111111111")).data!!

		assertThat(post.hasMedia).isFalse()
		assertThat(server.takeRequest().url.encodedPath).isEqualTo("/api/v1/posts/11111111-1111-4111-8111-111111111111")
	}

	@Test
	fun commentsKeepThreadingAndTakedownFields() = runTest {
		respond("comments_page.json")

		val page = retrofit.create(FeedApi::class.java).getPostComment(UUID.randomUUID(), page = 0).data!!

		val (root, reply, takenDown) = page.content
		assertThat(root.replyCount).isEqualTo(2)
		assertThat(root.canDelete).isTrue()
		assertThat(root.isReply).isFalse()
		assertThat(reply.isReply).isTrue()
		assertThat(reply.rootId).isEqualTo("c1")
		assertThat(reply.replyToUsername).isEqualTo("rizky")
		assertThat(reply.canDelete).isFalse() // absent means false
		assertThat(takenDown.isTakenDown).isTrue()
		assertThat(takenDown.commentContent).isNull()
	}

	@Test
	fun theRegisterOtpAnswerSaysWhetherVerificationIsNeeded() = runTest {
		respond("register_otp_required.json")
		respond("register_otp_not_required.json")
		val api = retrofit.create(AuthApi::class.java)

		val required = api.sendRegisterOtp(EmailRequest("r@mail.com")).data!!
		val notRequired = api.sendRegisterOtp(EmailRequest("r@mail.com")).data!!

		assertThat(required.verificationRequired).isTrue()
		assertThat(required.expirationTimeInMinute).isEqualTo("5")
		assertThat(notRequired.verificationRequired).isFalse()
		assertThat(notRequired.otpSendToEmailAddress).isNull()
	}

	@Test
	fun anUploadUrlKeepsTheSignedUrlAndTheKey() = runTest {
		respond("upload_url.json")

		val data = retrofit.create(PostApi::class.java).createUploadUrl(CreatePostUploadUrlRequest("image/jpeg")).data!!

		assertThat(data.presignedUrl).isEqualTo("https://storage.example/put?sig=abc")
		assertThat(data.objectKey).isEqualTo("posts/2026/11/abc.jpg")
		assertThat(data.expiresInSeconds).isEqualTo(900L)
	}

	@Test
	fun followStatusMapsTheCounters() = runTest {
		respond("follow_status.json")

		val status = retrofit.create(UserApi::class.java).getFollowStatus("u1").data!!

		assertThat(status.isFollowing).isTrue()
		assertThat(status.followerCount).isEqualTo(12L)
		assertThat(status.followingCount).isEqualTo(4L)
	}

	@Test
	fun anActionAnswerWithNullDataSucceedsWithTheMessage() = runTest {
		respond("action_without_data.json")

		val states = flowFromMessageApi { retrofit.create(FeedApi::class.java).likePost(UUID.randomUUID()) }.toList()

		assertThat(states.last()).isEqualTo(ResultState.Success("Liked"))
	}

	@Test
	fun aServerErrorBodyReachesTheScreenAsTheMessage() = runTest {
		respond("error_validation.json", code = 409)

		val states = flowFromApi { retrofit.create(AuthApi::class.java).login(LoginRequest("a", "b")) }.toList()

		assertThat((states.last() as ResultState.Failure).message).isEqualTo("Username sudah dipakai")
	}

	/**
	 * R4 in the test plan. Gson does not know Kotlin nullability: a missing required field parses to null in a
	 * non-null property, and the NPE only shows up later, far from here. This pins today's behaviour so the
	 * decision on validation (E8) has evidence; when validation exists, this test should expect a failure instead.
	 */
	@Test
	fun aMissingRequiredFieldCurrentlyParsesAsANullInANonNullProperty() = runTest {
		server.enqueue(
			MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
				.body("""{"code":"200","message":"OK","data":{"refreshToken":"r","username":"u","userId":"1"}}""").build()
		)

		val data = retrofit.create(AuthApi::class.java).login(LoginRequest("u", "p")).data!!

		@Suppress("USELESS_CAST", "KotlinConstantConditions")
		val token: String? = data.token as String?
		assertThat(token).isNull()
	}
}
