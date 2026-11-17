package com.rizkysiregar.narrativ.data.repository.impl

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.EmailRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetRepliesRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.DeleteAccountRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.narrativ.domain.moderation.ReportReason
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

/**
 * What each repository sends: the method, the path, the query and the body of the request its call makes, and how
 * it treats the answer. The json is the one in `src/test/resources/fixtures` where there is one.
 */
class RepositoryImplTest {

	private lateinit var server: MockWebServer
	private lateinit var retrofit: Retrofit
	private var body = ACTION_OK
	private var code = 200

	@Before
	fun setUp() {
		server = MockWebServer().apply {
			dispatcher = object : Dispatcher() {
				override fun dispatch(request: RecordedRequest) =
					MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
			}
			start()
		}
		retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
	}

	@After
	fun tearDown() = server.close()

	private fun fixture(name: String) = javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!.bufferedReader().use { it.readText() }

	private fun emptyPage(last: Boolean = true) =
		"""{"code":"200","message":"OK","data":{"pageNumber":0,"last":$last,"totalPages":1,"pageSize":0,"totalElements":0,"content":[]}}"""

	/** The request the last call made, as "METHOD /path?query". */
	private fun sent(): RecordedRequest = server.takeRequest()

	private fun RecordedRequest.line() = "$method ${url.encodedPath.removePrefix("/api/v1")}" + (url.query?.let { "?$it" } ?: "")

	private suspend fun <T> Flow<ResultState<T>>.last(): ResultState<T> = toList().last()

	private val id = UUID.fromString("11111111-1111-4111-8111-111111111111")

	// --- feed ---

	@Test
	fun feedPagesAreRequestedByPageAndSize() = runTest {
		body = fixture("feed_page.json")
		val repo = FeedRepositoryImpl(retrofit)

		val result = repo.getAllFeed(GetAllFeedRequest(page = 2, size = 10)).last()

		assertThat(sent().line()).isEqualTo("GET /posts?page=2&size=10")
		assertThat((result as ResultState.Success).data.content).hasSize(2)
	}

	@Test
	fun exploreAndTheProfileGridCanAskForMediaOnly() = runTest {
		body = fixture("feed_page.json")
		val repo = FeedRepositoryImpl(retrofit)

		repo.getExplorePosts(GetAllFeedRequest(page = 0, size = 20, mediaOnly = true)).last()
		repo.getFeedByUsername(GetFeedByUsernameRequest(page = 1, size = 9, username = "rizky", mediaOnly = true)).last()

		assertThat(sent().line()).isEqualTo("GET /posts/explore?page=0&size=20&mediaOnly=true")
		assertThat(sent().line()).isEqualTo("GET /posts/users/rizky/posts?page=1&size=9&mediaOnly=true")
	}

	@Test
	fun aSinglePostIsRequestedById() = runTest {
		body = fixture("post_single.json")

		val result = FeedRepositoryImpl(retrofit).getPost(id).last()

		assertThat(sent().line()).isEqualTo("GET /posts/$id")
		assertThat((result as ResultState.Success).data.id).isEqualTo(id.toString())
	}

	@Test
	fun likeAndDislikeUseTheirOwnMethodsAndPaths() = runTest {
		val repo = FeedRepositoryImpl(retrofit)

		val liked = repo.likePost(id).last()
		repo.dislikePost(id).last()

		assertThat(sent().line()).isEqualTo("POST /posts/$id/like")
		assertThat(sent().line()).isEqualTo("DELETE /posts/$id/dislike")
		assertThat(liked).isEqualTo(ResultState.Success("ok"))
	}

	@Test
	fun aCommentIsPostedToItsPostWithTheParentForAReply() = runTest {
		val parent = UUID.randomUUID()

		FeedRepositoryImpl(retrofit).addComment(AddCommentRequest(postId = id, content = "balas", parentId = parent)).last()

		val request = sent()
		assertThat(request.line()).isEqualTo("POST /posts/$id/comments")
		assertThat(request.body!!.utf8()).contains("\"content\":\"balas\"")
		assertThat(request.body!!.utf8()).contains("\"parentId\":\"$parent\"")
	}

	@Test
	fun commentsRepliesAndDeletionHaveTheirPaths() = runTest {
		body = fixture("comments_page.json")
		val repo = FeedRepositoryImpl(retrofit)

		repo.getComment(GetCommentRequest(postId = id, page = 0)).last()
		repo.getReplies(GetRepliesRequest(commentId = id, page = 1)).last()
		repo.deleteComment(id).last()

		assertThat(sent().line()).isEqualTo("GET /posts/$id/comments?page=0")
		assertThat(sent().line()).isEqualTo("GET /comments/$id/replies?page=1")
		assertThat(sent().line()).isEqualTo("DELETE /comments/$id")
	}

	@Test
	fun aServerErrorBecomesAFailureWithItsMessage() = runTest {
		code = 500
		body = """{"code":"500","message":"DB down","data":null}"""

		val result = FeedRepositoryImpl(retrofit).getAllFeed(GetAllFeedRequest(0, 10)).last()

		assertThat((result as ResultState.Failure).message).isEqualTo("DB down")
	}

	// --- auth ---

	@Test
	fun theAuthCallsGoToTheirEndpointsWithTheirBodies() = runTest {
		val repo = AuthRepositoryImpl(retrofit)

		body = fixture("login_success.json")
		repo.login(LoginRequest("rizky", "secret")).last()
		body = fixture("register_otp_required.json")
		repo.sendRegisterOtp("r@mail.com").last()
		body = """{"code":"200","message":"OK","data":{"registrationToken":"reg"}}"""
		repo.verifyRegisterOtp(RegisterVerifyOtpRequest("r@mail.com", "123456")).last()
		body = ACTION_OK
		repo.register(RegisterRequest("rizky", "pw", "r@mail.com", "R", "S", "reg")).last()
		body = """{"code":"200","message":"OK","data":{"otpSendToEmailAddress":"r***","expirationTimeInMinute":"5"}}"""
		repo.forgotPassword("r@mail.com").last()
		body = """{"code":"200","message":"OK","data":{"resetToken":"rt"}}"""
		repo.verifyOtp(ResetPassVerifyOtpRequest("r@mail.com", "123456")).last()
		body = ACTION_OK
		repo.resetNewPassword(ResetNewPasswordRequest("r@mail.com", "rt", "Kuat123!")).last()

		val requests = (1..7).map { sent() }
		assertThat(requests.map { it.line() }).containsExactly(
			"POST /auth/login", "POST /auth/register/otp", "POST /auth/register/verify-otp", "POST /auth/register",
			"POST /auth/forgot-password", "POST /auth/verify-otp", "POST /auth/reset-password"
		).inOrder()
		assertThat(requests[1].body!!.utf8()).contains("\"email\":\"r@mail.com\"")
		assertThat(requests[3].body!!.utf8()).contains("\"registrationToken\":\"reg\"")
	}

	@Test
	fun theGoogleIdTokenIsSentToTheGoogleEndpoint() = runTest {
		body = fixture("login_success.json")

		AuthRepositoryImpl(retrofit).googleLogin(com.rizkysiregar.narrativ.data.remote.model.request.auth.GoogleLoginRequest("id-token")).last()

		val request = sent()
		assertThat(request.line()).isEqualTo("POST /auth/google")
		assertThat(request.body!!.utf8()).contains("\"idToken\":\"id-token\"")
	}

	// --- user ---

	@Test
	fun followUnfollowAndStatusUseTheUserPaths() = runTest {
		val repo = UserRepositoryImpl(retrofit)

		repo.follow("u1").last()
		repo.unfollow("u1").last()
		body = fixture("follow_status.json")
		val status = repo.getFollowStatus("u1").last()

		assertThat(sent().line()).isEqualTo("POST /users/u1/follow")
		assertThat(sent().line()).isEqualTo("DELETE /users/u1/follow")
		assertThat(sent().line()).isEqualTo("GET /users/u1/follow-status")
		assertThat((status as ResultState.Success).data.followerCount).isEqualTo(12L)
	}

	@Test
	fun followersAndFollowingArePaged() = runTest {
		body = emptyPage()
		val repo = UserRepositoryImpl(retrofit)

		repo.getFollowers("u1", page = 0, size = 20).last()
		repo.getFollowing("u1", page = 2, size = 20).last()

		assertThat(sent().line()).isEqualTo("GET /users/u1/followers?page=0&size=20")
		assertThat(sent().line()).isEqualTo("GET /users/u1/following?page=2&size=20")
	}

	@Test
	fun deletingTheAccountSendsThePasswordOrTheGoogleToken() = runTest {
		val repo = UserRepositoryImpl(retrofit)

		repo.deleteAccount(DeleteAccountRequest(password = "secret")).last()
		repo.deleteAccount(DeleteAccountRequest(idToken = "proof")).last()

		val first = sent()
		assertThat(first.line()).isEqualTo("POST /users/me/deletion")
		assertThat(first.body!!.utf8()).contains("\"password\":\"secret\"")
		assertThat(sent().body!!.utf8()).contains("\"idToken\":\"proof\"")
	}

	@Test
	fun searchingAsksByUsernameAndNoMatchIsAnEmptyPageNotAnError() = runTest {
		val repo = UserRepositoryImpl(retrofit)
		body = emptyPage()
		repo.searchUsers(SearchUserRequest(username = "riz", page = 0, size = 20)).last()
		assertThat(sent().line()).isEqualTo("GET /users?username=riz&page=0&size=20")

		code = 404
		body = """{"code":"404","message":"none","data":null}"""
		val result = repo.searchUsers(SearchUserRequest(username = "zzz", page = 3, size = 20)).last()

		val page = (result as ResultState.Success).data
		assertThat(page.content).isEmpty()
		assertThat(page.last).isTrue()
		assertThat(page.pageNumber).isEqualTo(3)
	}

	// --- moderation ---

	@Test
	fun aReportSendsTheTargetTheReasonAndATrimmedDescription() = runTest {
		ModerationRepositoryImpl(retrofit).report(ReportTarget.COMMENT, "c1", ReportReason.OTHER, "  kasar  ").last()

		val request = sent()
		assertThat(request.line()).isEqualTo("POST /reports")
		val json = request.body!!.utf8()
		assertThat(json).contains("\"targetType\":\"COMMENT\"")
		assertThat(json).contains("\"targetId\":\"c1\"")
		assertThat(json).contains("\"reason\":\"OTHER\"")
		assertThat(json).contains("\"description\":\"kasar\"")
	}

	@Test
	fun aBlankDescriptionIsLeftOut() = runTest {
		ModerationRepositoryImpl(retrofit).report(ReportTarget.POST, "p1", ReportReason.SPAM, "   ").last()

		assertThat(sent().body!!.utf8()).doesNotContain("description")
	}

	@Test
	fun blockUnblockAndTheBlockedListUseTheirPaths() = runTest {
		val repo = ModerationRepositoryImpl(retrofit)

		repo.blockUser("u1").last()
		repo.unblockUser("u1").last()
		body = emptyPage()
		repo.getBlockedUsers(page = 1, size = 20).last()

		assertThat(sent().line()).isEqualTo("POST /users/u1/block")
		assertThat(sent().line()).isEqualTo("DELETE /users/u1/block")
		assertThat(sent().line()).isEqualTo("GET /users/me/blocks?page=1&size=20")
	}

	@Test
	fun noBlockedUsersIsAnEmptyPageNotAnError() = runTest {
		code = 404
		body = """{"code":"404","message":"none","data":null}"""

		val result = ModerationRepositoryImpl(retrofit).getBlockedUsers(page = 0, size = 20).last()

		assertThat((result as ResultState.Success).data.content).isEmpty()
	}

	// --- posts ---

	@Test
	fun anUploadUrlIsRequestedForAContentType() = runTest {
		body = fixture("upload_url.json")

		val response = PostRepositoryImpl(retrofit).createUploadUrl("video/mp4")

		val request = sent()
		assertThat(request.line()).isEqualTo("POST /posts/upload-url")
		assertThat(request.body!!.utf8()).contains("\"contentType\":\"video/mp4\"")
		assertThat(response.objectKey).isEqualTo("posts/2026/11/abc.jpg")
	}

	@Test
	fun aBlankCaptionIsSentAsNoCaption() = runTest {
		body = """{"code":"200","message":"OK","data":{"status":"CREATED"}}"""
		val repo = PostRepositoryImpl(retrofit)

		repo.createPost("   ", listOf("k1", "k2"))
		repo.createPost("halo", emptyList())

		val first = sent().body!!.utf8()
		assertThat(first).doesNotContain("caption")
		assertThat(first).contains("\"mediaKeys\":[\"k1\",\"k2\"]")
		assertThat(sent().body!!.utf8()).contains("\"caption\":\"halo\"")
	}

	@Test
	fun anAnswerWithoutDataIsAnErrorWithTheServerMessage() = runTest {
		body = """{"code":"200","message":"tidak ada data","data":null}"""
		val repo = PostRepositoryImpl(retrofit)

		val uploadUrl = assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { repo.createUploadUrl("image/png") } }
		val post = assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { repo.createPost("x", emptyList()) } }

		assertThat(uploadUrl).hasMessageThat().isEqualTo("tidak ada data")
		assertThat(post).hasMessageThat().isEqualTo("tidak ada data")
	}

	private companion object {
		const val ACTION_OK = """{"code":"200","message":"ok","data":null}"""
	}
}
