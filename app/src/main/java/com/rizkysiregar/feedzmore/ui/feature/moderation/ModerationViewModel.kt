package com.rizkysiregar.feedzmore.ui.feature.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.repository.ModerationRepository
import com.rizkysiregar.feedzmore.domain.moderation.ReportReason
import com.rizkysiregar.feedzmore.domain.moderation.ReportTarget
import com.rizkysiregar.feedzmore.telemetry.AnalyticsEvents
import com.rizkysiregar.feedzmore.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import com.rizkysiregar.feedzmore.i18n.AppStrings
import com.rizkysiregar.feedzmore.R

/**
 * Outcome of a report or a block. [message] is only set when the global error dialog did not already
 * tell the user (it covers HTTP errors and a missing connection, except 404).
 */
sealed interface ModerationResult {
	data object Success : ModerationResult
	data class Failure(val message: String?) : ModerationResult
}

@HiltViewModel
class ModerationViewModel @Inject constructor(
	private val moderationRepository: ModerationRepository,
	dataStore: DataStoreManager,
	private val analytics: AppAnalytics,
	private val appStrings: AppStrings
) : ViewModel() {

	/** Who is looking, so a user is never offered to report or block their own content. */
	val currentUserId: StateFlow<String?> = dataStore.get(PrefKeys.userId, "")
		.map { it.ifEmpty { null } }
		.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	fun report(
		target: ReportTarget,
		targetId: String,
		reason: ReportReason,
		description: String?,
		onResult: (ModerationResult) -> Unit
	) {
		viewModelScope.launch {
			moderationRepository.report(target, targetId, reason, description).collect { result ->
				if (result is ResultState.Success) {
					analytics.log(
						AnalyticsEvents.CONTENT_REPORTED,
						AnalyticsEvents.Param.TARGET to target.apiName,
						AnalyticsEvents.Param.REASON to reason.apiName
					)
				}
				result.toModerationResult(notFoundMessage = appStrings.get(R.string.error_content_unavailable))?.let(onResult)
			}
		}
	}

	fun block(userId: String, onResult: (ModerationResult) -> Unit) {
		viewModelScope.launch {
			moderationRepository.blockUser(userId).collect { result ->
				if (result is ResultState.Success) analytics.log(AnalyticsEvents.USER_BLOCKED)
				result.toModerationResult(notFoundMessage = appStrings.get(R.string.error_account_unavailable))?.let(onResult)
			}
		}
	}

	private fun ResultState<String>.toModerationResult(notFoundMessage: String): ModerationResult? = when (this) {
		is ResultState.Success -> ModerationResult.Success
		is ResultState.Failure -> {
			val notFound = (throwable as? HttpException)?.code() == 404
			ModerationResult.Failure(if (notFound) notFoundMessage else null)
		}
		else -> null
	}
}
