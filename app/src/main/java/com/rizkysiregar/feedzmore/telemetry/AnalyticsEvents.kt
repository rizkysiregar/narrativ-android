package com.rizkysiregar.feedzmore.telemetry

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Event and parameter names sent to Firebase Analytics. Keep names snake_case, max 40 chars.
 */
object AnalyticsEvents {
	// auth funnel
	const val LOGIN = "login"
	const val LOGIN_FAILED = "login_failed"
	const val SIGN_UP = "sign_up"
	const val SIGN_UP_FAILED = "sign_up_failed"
	const val REGISTER_OTP_SENT = "register_otp_sent"
	const val REGISTER_OTP_VERIFIED = "register_otp_verified"
	const val REGISTER_OTP_FAILED = "register_otp_failed"
	const val ONBOARDING_COMPLETED = "onboarding_completed"

	// content
	const val POST_SUBMITTED = "post_submitted"
	const val POST_UPLOAD_SUCCEEDED = "post_upload_succeeded"
	const val POST_UPLOAD_FAILED = "post_upload_failed"
	const val COMMENT_ADDED = "comment_added"
	const val COMMENT_FAILED = "comment_failed"
	const val SEARCH = "search"

	// moderation
	const val CONTENT_REPORTED = "content_reported"
	const val USER_BLOCKED = "user_blocked"

	// session
	const val SESSION_EXPIRED = "session_expired"
	const val FORCE_UPDATE_SHOWN = "force_update_shown"

	object Param {
		const val METHOD = "method"
		const val MEDIA_COUNT = "media_count"
		const val HAS_VIDEO = "has_video"
		const val HAS_CAPTION = "has_caption"
		const val IS_REPLY = "is_reply"
		const val REASON = "reason"
		const val TARGET = "target"
		const val STAGE = "stage"
		const val ERROR_CODE = "error_code"
	}
}
