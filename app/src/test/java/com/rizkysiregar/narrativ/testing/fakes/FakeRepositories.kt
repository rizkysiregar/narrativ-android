package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.data.remote.model.request.user.ConfirmEmailChangeRequest
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.GoogleLoginRequest
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
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.repository.AuthRepository
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import com.rizkysiregar.narrativ.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/*
 * Repositories as scripted fakes: a test assigns what a call returns (a flow it can control) and reads what was
 * asked. A call nobody scripted fails loudly instead of returning something plausible.
 */

private fun <T> unscripted(name: String): T = error("$name was called but the test did not script it")

fun <T> successOf(data: T): Flow<ResultState<T>> = flowOf(ResultState.Loading, ResultState.Success(data))

fun <T> failureOf(message: String, throwable: Throwable? = null): Flow<ResultState<T>> =
	flowOf(ResultState.Loading, ResultState.Failure(message, throwable))

class FakeAuthRepository : AuthRepository {
	var onLogin: (LoginRequest) -> Flow<ResultState<LoginResponse>> = { unscripted("login") }
	var onGoogleLogin: (GoogleLoginRequest) -> Flow<ResultState<LoginResponse>> = { unscripted("googleLogin") }
	var onRegister: (RegisterRequest) -> Flow<ResultState<String>> = { unscripted("register") }
	var onSendRegisterOtp: (String) -> Flow<ResultState<RegisterOtpResponse>> = { unscripted("sendRegisterOtp") }
	var onVerifyRegisterOtp: (RegisterVerifyOtpRequest) -> Flow<ResultState<RegistrationTokenResponse>> = { unscripted("verifyRegisterOtp") }
	var onForgotPassword: (String) -> Flow<ResultState<ForgotPasswordResponse>> = { unscripted("forgotPassword") }
	var onVerifyOtp: (ResetPassVerifyOtpRequest) -> Flow<ResultState<ResetPassVerifyOtpResponse>> = { unscripted("verifyOtp") }
	var onResetNewPassword: (ResetNewPasswordRequest) -> Flow<ResultState<String>> = { unscripted("resetNewPassword") }

	val logins = mutableListOf<LoginRequest>()
	val googleLogins = mutableListOf<GoogleLoginRequest>()
	val registers = mutableListOf<RegisterRequest>()
	val registerOtpEmails = mutableListOf<String>()
	val verifyRegisterOtps = mutableListOf<RegisterVerifyOtpRequest>()

	override fun login(request: LoginRequest) = onLogin(request).also { logins += request }
	override fun googleLogin(request: GoogleLoginRequest) = onGoogleLogin(request).also { googleLogins += request }
	override fun register(registerRequest: RegisterRequest) = onRegister(registerRequest).also { registers += registerRequest }
	override fun sendRegisterOtp(email: String) = onSendRegisterOtp(email).also { registerOtpEmails += email }
	override fun verifyRegisterOtp(request: RegisterVerifyOtpRequest) = onVerifyRegisterOtp(request).also { verifyRegisterOtps += request }
	override fun forgotPassword(email: String) = onForgotPassword(email)
	override fun verifyOtp(request: ResetPassVerifyOtpRequest) = onVerifyOtp(request)
	override fun resetNewPassword(request: ResetNewPasswordRequest) = onResetNewPassword(request)
}

class FakeFeedRepository : FeedRepository {
	var onLike: (UUID) -> Flow<ResultState<String>> = { successOf("Liked") }
	var onDislike: (UUID) -> Flow<ResultState<String>> = { successOf("Disliked") }
	var onGetPost: (UUID) -> Flow<ResultState<FeedContentItem>> = { unscripted("getPost") }
	var onAddComment: (AddCommentRequest) -> Flow<ResultState<String>> = { unscripted("addComment") }
	var onDeleteComment: (UUID) -> Flow<ResultState<String>> = { unscripted("deleteComment") }
	var onPinComment: (UUID, UUID) -> Flow<ResultState<String>> = { _, _ -> unscripted("pinComment") }
	var onUnpinComment: (UUID) -> Flow<ResultState<String>> = { unscripted("unpinComment") }
	var onGetComment: (GetCommentRequest) -> Flow<ResultState<PaginationResponseWrapper<CommentItem>>> = { unscripted("getComment") }
	var onGetReplies: (GetRepliesRequest) -> Flow<ResultState<PaginationResponseWrapper<CommentItem>>> = { unscripted("getReplies") }

	val likes = mutableListOf<UUID>()
	val dislikes = mutableListOf<UUID>()
	val addedComments = mutableListOf<AddCommentRequest>()
	val commentRequests = mutableListOf<GetCommentRequest>()
	val replyRequests = mutableListOf<GetRepliesRequest>()

	override fun getAllFeed(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>> = unscripted("getAllFeed")
	override fun getExplorePosts(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>> = unscripted("getExplorePosts")
	override fun getFeedByUsername(request: GetFeedByUsernameRequest): Flow<ResultState<FeedsPostPaginationResponse>> = unscripted("getFeedByUsername")
	override fun getPost(postId: UUID) = onGetPost(postId)
	override fun likePost(postId: UUID) = onLike(postId).also { likes += postId }
	override fun dislikePost(postId: UUID) = onDislike(postId).also { dislikes += postId }
	override fun addComment(request: AddCommentRequest) = onAddComment(request).also { addedComments += request }
	override fun deleteComment(commentId: UUID) = onDeleteComment(commentId)
	override fun pinComment(postId: UUID, commentId: UUID) = onPinComment(postId, commentId)
	override fun unpinComment(postId: UUID) = onUnpinComment(postId)
	override fun getComment(request: GetCommentRequest) = onGetComment(request).also { commentRequests += request }
	override fun getReplies(request: GetRepliesRequest) = onGetReplies(request).also { replyRequests += request }
}

class FakeUserRepository : UserRepository {
	var onDeleteAccount: (DeleteAccountRequest) -> Flow<ResultState<String>> = { unscripted("deleteAccount") }
	val deleteRequests = mutableListOf<DeleteAccountRequest>()
	var onGetUserProfile: (String) -> Flow<ResultState<UserSearchItemResponse>> = { unscripted("getUserProfile") }
	val profileRequests = mutableListOf<String>()

	override fun deleteAccount(request: DeleteAccountRequest) = onDeleteAccount(request).also { deleteRequests += request }

	var onSendEmailChangeOtp: (String) -> Flow<ResultState<ForgotPasswordResponse>> = { unscripted("sendEmailChangeOtp") }
	val emailChangeOtpRequests = mutableListOf<String>()
	var onConfirmEmailChange: (ConfirmEmailChangeRequest) -> Flow<ResultState<LoginResponse>> = { unscripted("confirmEmailChange") }
	val emailChangeConfirmations = mutableListOf<ConfirmEmailChangeRequest>()

	override fun sendEmailChangeOtp(email: String) = onSendEmailChangeOtp(email).also { emailChangeOtpRequests += email }
	override fun confirmEmailChange(request: ConfirmEmailChangeRequest) =
		onConfirmEmailChange(request).also { emailChangeConfirmations += request }

	var onCreateProfileImageUploadUrl: (String) -> ProfileImageUploadUrlResponse = { unscripted("createProfileImageUploadUrl") }
	var onSetProfileImage: (String) -> UserSearchItemResponse = { unscripted("setProfileImage") }
	var onDeleteProfileImage: () -> UserSearchItemResponse = { unscripted("deleteProfileImage") }
	val profileImageUploadUrlRequests = mutableListOf<String>()
	val profileImageKeys = mutableListOf<String>()
	var profileImageDeletions = 0

	override suspend fun createProfileImageUploadUrl(contentType: String) =
		onCreateProfileImageUploadUrl(contentType).also { profileImageUploadUrlRequests += contentType }

	override suspend fun setProfileImage(objectKey: String) = onSetProfileImage(objectKey).also { profileImageKeys += objectKey }

	override suspend fun deleteProfileImage() = onDeleteProfileImage().also { profileImageDeletions++ }
	override fun getUserProfile(userId: String) = onGetUserProfile(userId).also { profileRequests += userId }
	override fun searchUsers(request: SearchUserRequest): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>> = unscripted("searchUsers")
	override fun follow(userId: String): Flow<ResultState<String>> = unscripted("follow")
	override fun unfollow(userId: String): Flow<ResultState<String>> = unscripted("unfollow")
	override fun getFollowStatus(userId: String): Flow<ResultState<FollowStatusResponse>> = unscripted("getFollowStatus")
	override fun getFollowers(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> = unscripted("getFollowers")
	override fun getFollowing(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> = unscripted("getFollowing")
}
