package com.rizkysiregar.narrativ.ui.feature.home.notifications

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationItem
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationPage
import com.rizkysiregar.narrativ.data.remote.model.response.notification.UnreadCountResponse
import com.rizkysiregar.narrativ.data.repository.NotificationRepository
import com.rizkysiregar.narrativ.domain.notification.UnreadNotifications
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	private class FakeNotificationRepository : NotificationRepository {
		var pages: (String?) -> Flow<ResultState<NotificationPage>> = { successOf(NotificationPage()) }
		var unread = 3
		var markAllReadResult: Flow<ResultState<String>> = successOf("ok")
		val cursors = mutableListOf<String?>()
		var markAllReadCalls = 0

		override fun getNotifications(cursor: String?, size: Int) = pages(cursor).also { cursors += cursor }
		override fun getUnreadCount() = successOf(UnreadCountResponse(unread))
		override fun markAllRead() = markAllReadResult.also { markAllReadCalls++ }
		override fun markRead(groupKey: String) = successOf("ok")
	}

	private val repo = FakeNotificationRepository()

	private fun item(key: String, unread: Boolean = true) = NotificationItem(groupKey = key, type = "POST_LIKED", unread = unread)

	private fun TestScope.newViewModel(): Pair<NotificationsViewModel, UnreadNotifications> {
		val badge = UnreadNotifications(repo, backgroundScope)
		return NotificationsViewModel(repo, badge) to badge
	}

	@Test
	fun openingTheInboxLoadsItMarksItReadAndClearsTheBadgeButKeepsWhatIsNew() = runTest(mainRule.dispatcher) {
		repo.pages = { successOf(NotificationPage(listOf(item("a"), item("b", unread = false)), last = true)) }

		val (vm, badge) = newViewModel()

		assertThat(vm.uiState.value.items.map { it.unread }).containsExactly(true, false).inOrder()
		assertThat(repo.markAllReadCalls).isEqualTo(1)
		assertThat(badge.count.value).isEqualTo(0)
	}

	@Test
	fun theNextPageFollowsTheCursorUntilTheLastOne() = runTest(mainRule.dispatcher) {
		repo.pages = { cursor ->
			if (cursor == null) successOf(NotificationPage(listOf(item("a")), last = false, nextCursor = "c1"))
			else successOf(NotificationPage(listOf(item("b")), last = true))
		}
		val (vm, _) = newViewModel()

		vm.loadMore()
		vm.loadMore() // the last page was reached, no request

		assertThat(vm.uiState.value.items.map { it.groupKey }).containsExactly("a", "b").inOrder()
		assertThat(repo.cursors).containsExactly(null, "c1").inOrder()
	}

	@Test
	fun anOpenedEntryIsNoLongerNew() = runTest(mainRule.dispatcher) {
		repo.pages = { successOf(NotificationPage(listOf(item("a")), last = true)) }
		val (vm, _) = newViewModel()

		vm.onOpened(vm.uiState.value.items.single())

		assertThat(vm.uiState.value.items.single().unread).isFalse()
	}

	@Test
	fun whenMarkingReadFailsTheBadgeComesBack() = runTest(mainRule.dispatcher) {
		repo.markAllReadResult = failureOf("offline")

		val (_, badge) = newViewModel()

		assertThat(badge.count.value).isEqualTo(3)
	}

	@Test
	fun aFailedFirstPageCanBeRetried() = runTest(mainRule.dispatcher) {
		var fail = true
		repo.pages = { if (fail) failureOf("offline") else successOf(NotificationPage(listOf(item("a")), last = true)) }
		val (vm, _) = newViewModel()
		assertThat(vm.uiState.value.errorMessage).isEqualTo("offline")

		fail = false
		vm.retry()

		assertThat(vm.uiState.value.items).hasSize(1)
		assertThat(vm.uiState.value.errorMessage).isNull()
	}
}
