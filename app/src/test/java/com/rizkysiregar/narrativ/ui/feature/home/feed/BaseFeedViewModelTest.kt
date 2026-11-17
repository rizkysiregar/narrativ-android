package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.aFeedItem
import com.rizkysiregar.narrativ.testing.aFeedPage
import com.rizkysiregar.narrativ.testing.fakes.FakeFeedRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

/** Paging and optimistic likes of every feed screen, through a thin subclass that serves scripted pages. */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseFeedViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val repo = FakeFeedRepository()

	private class TestFeedViewModel(
		dataStore: DataStoreManager,
		repo: FakeFeedRepository,
		val serve: (page: Int, mediaOnly: Boolean) -> Flow<ResultState<FeedsPostPaginationResponse>>
	) : BaseFeedViewModel(dataStore, DislikePostUseCase(repo), LikePostUseCase(repo)) {
		val requested = mutableListOf<Pair<Int, Boolean>>()

		override suspend fun fetchPage(page: Int, size: Int, mediaOnly: Boolean): Flow<ResultState<FeedsPostPaginationResponse>> {
			requested += page to mediaOnly
			return serve(page, mediaOnly)
		}
	}

	private fun TestScope.feed(serve: (Int, Boolean) -> Flow<ResultState<FeedsPostPaginationResponse>>) =
		TestFeedViewModel(testDataStoreManager(backgroundScope, folder.preferencesFile()), repo, serve)

	private fun pageOf(vararg items: FeedContentItem, last: Boolean = false) = successOf(aFeedPage(items.toList(), last = last))

	private fun <T> pending(deferred: CompletableDeferred<ResultState<T>>): Flow<ResultState<T>> = flow {
		emit(ResultState.Loading)
		emit(deferred.await())
	}

	// --- paging ---

	@Test
	fun refreshLoadsTheFirstPage() = runTest(mainRule.dispatcher) {
		val a = aFeedItem()
		val b = aFeedItem()
		val vm = feed { _, _ -> pageOf(a, b) }

		vm.refresh()

		val state = vm.uiState.value
		assertThat(state.items).containsExactly(a, b).inOrder()
		assertThat(state.page).isEqualTo(1)
		assertThat(state.hasMore).isTrue()
		assertThat(state.hasLoadedInitialPage).isTrue()
		assertThat(state.isRefreshing).isFalse()
		assertThat(vm.gridState.value.items).isEmpty() // the grid is a separate stream, still untouched
		assertThat(vm.requested).containsExactly(0 to false)
	}

	@Test
	fun theLastPageEndsPaging() = runTest(mainRule.dispatcher) {
		val vm = feed { _, _ -> pageOf(aFeedItem(), last = true) }
		vm.refresh()

		vm.loadNextPage()

		assertThat(vm.uiState.value.hasMore).isFalse()
		assertThat(vm.requested).hasSize(1)
	}

	@Test
	fun anEmptyPageEndsPagingEvenIfTheServerSaysThereIsMore() = runTest(mainRule.dispatcher) {
		val vm = feed { _, _ -> pageOf(last = false) }

		vm.refresh()

		assertThat(vm.uiState.value.hasMore).isFalse()
	}

	@Test
	fun nextPagesAreAppendedInOrder() = runTest(mainRule.dispatcher) {
		val first = listOf(aFeedItem(), aFeedItem())
		val second = aFeedItem()
		val vm = feed { page, _ -> if (page == 0) pageOf(*first.toTypedArray()) else pageOf(second, last = true) }
		vm.refresh()

		vm.loadNextPage()

		assertThat(vm.uiState.value.items).containsExactly(first[0], first[1], second).inOrder()
		assertThat(vm.uiState.value.page).isEqualTo(2)
		assertThat(vm.uiState.value.hasMore).isFalse()
		assertThat(vm.requested.map { it.first }).containsExactly(0, 1).inOrder()
	}

	@Test
	fun nextPageDoesNothingBeforeTheFirstPageArrived() = runTest(mainRule.dispatcher) {
		val vm = feed { _, _ -> pageOf(aFeedItem()) }

		vm.loadNextPage()

		assertThat(vm.requested).isEmpty()
	}

	@Test
	fun scrollingTwiceWhileAPageLoadsAsksOnce() = runTest(mainRule.dispatcher) {
		val slow = CompletableDeferred<ResultState<FeedsPostPaginationResponse>>()
		val vm = feed { page, _ -> if (page == 0) pageOf(aFeedItem()) else pending(slow) }
		vm.refresh()

		vm.loadNextPage()
		vm.loadNextPage()

		assertThat(vm.requested.map { it.first }).containsExactly(0, 1).inOrder()
		assertThat(vm.uiState.value.isLoadingMore).isTrue()
		slow.complete(ResultState.Success(aFeedPage(listOf(aFeedItem()), last = true)))
		assertThat(vm.uiState.value.isLoadingMore).isFalse()
	}

	@Test
	fun aFailedFirstPageShowsTheErrorAndNoItems() = runTest(mainRule.dispatcher) {
		val vm = feed { _, _ -> failureOf("Gagal memuat") }

		vm.refresh()

		val state = vm.uiState.value
		assertThat(state.isError).isTrue()
		assertThat(state.errorMessage).isEqualTo("Gagal memuat")
		assertThat(state.isRefreshing).isFalse()
		assertThat(state.items).isEmpty()
	}

	@Test
	fun aFailedNextPageKeepsTheItemsAndCanBeRetried() = runTest(mainRule.dispatcher) {
		val first = aFeedItem()
		val second = aFeedItem()
		var attempts = 0
		val vm = feed { page, _ ->
			when {
				page == 0 -> pageOf(first)
				attempts++ == 0 -> failureOf("Putus")
				else -> pageOf(second, last = true)
			}
		}
		vm.refresh()

		vm.loadNextPage()
		assertThat(vm.uiState.value.items).containsExactly(first)
		assertThat(vm.uiState.value.isError).isTrue()
		assertThat(vm.uiState.value.isLoadingMore).isFalse()

		vm.loadNextPage()
		assertThat(vm.uiState.value.items).containsExactly(first, second).inOrder()
		assertThat(vm.uiState.value.isError).isFalse()
	}

	@Test
	fun refreshStartsOverFromThePageZero() = runTest(mainRule.dispatcher) {
		val old = aFeedItem()
		val fresh = aFeedItem()
		var round = 0
		val vm = feed { page, _ -> if (round == 0) pageOf(old) else pageOf(fresh, last = true).also { if (page != 0) error("must restart at 0") } }
		vm.refresh()
		round = 1

		vm.refresh()

		assertThat(vm.uiState.value.items).containsExactly(fresh)
		assertThat(vm.uiState.value.page).isEqualTo(1)
	}

	@Test
	fun aRefreshThatArrivesLateDoesNotOverwriteANewerOne() = runTest(mainRule.dispatcher) {
		val slowA = CompletableDeferred<ResultState<FeedsPostPaginationResponse>>()
		val newer = aFeedItem()
		var call = 0
		val vm = feed { _, _ -> if (call++ == 0) pending(slowA) else pageOf(newer, last = true) }

		vm.refresh()
		vm.refresh()
		slowA.complete(ResultState.Success(aFeedPage(listOf(aFeedItem(), aFeedItem()))))

		assertThat(vm.uiState.value.items).containsExactly(newer)
	}

	@Test
	fun theGridAsksForMediaOnlyAndLeavesTheListAlone() = runTest(mainRule.dispatcher) {
		val vm = feed { _, mediaOnly -> if (mediaOnly) pageOf(aFeedItem(), last = true) else pageOf(aFeedItem(), aFeedItem()) }

		vm.refreshGrid()

		assertThat(vm.requested).containsExactly(0 to true)
		assertThat(vm.gridState.value.items).hasSize(1)
		assertThat(vm.uiState.value.items).isEmpty()
	}

	// --- likes ---

	@Test
	fun aLikeShowsAtOnceBeforeTheServerAnswers() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = false, likeCount = 4)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onLike = { pending(answer) }
		val vm = feed { _, _ -> pageOf(post) }
		vm.refresh()

		vm.toggleLike(post.id)

		assertThat(vm.uiState.value.items.single().liked).isTrue()
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(5)
		answer.complete(ResultState.Success("Liked"))
		assertThat(vm.uiState.value.items.single().liked).isTrue() // success keeps it
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(5)
		assertThat(repo.likes).containsExactly(UUID.fromString(post.id))
	}

	@Test
	fun aRejectedLikeIsTakenBack() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = false, likeCount = 4)
		repo.onLike = { failureOf("Gagal") }
		val vm = feed { _, _ -> pageOf(post) }
		vm.refresh()

		vm.toggleLike(post.id)

		assertThat(vm.uiState.value.items.single().liked).isFalse()
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(4)
	}

	@Test
	fun tappingALikedPostUnlikesItThroughDislike() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = true, likeCount = 4)
		val vm = feed { _, _ -> pageOf(post) }
		vm.refresh()

		vm.toggleLike(post.id)

		assertThat(vm.uiState.value.items.single().liked).isFalse()
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(3)
		assertThat(repo.dislikes).containsExactly(UUID.fromString(post.id))
		assertThat(repo.likes).isEmpty()
	}

	@Test
	fun aSecondTapWhileTheFirstIsInFlightIsIgnored() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = false, likeCount = 4)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onLike = { pending(answer) }
		val vm = feed { _, _ -> pageOf(post) }
		vm.refresh()

		vm.toggleLike(post.id)
		vm.toggleLike(post.id)

		assertThat(repo.likes).hasSize(1)
		assertThat(repo.dislikes).isEmpty()
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(5)
	}

	@Test
	fun theNextTapWorksOnceTheFirstSettled() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = false, likeCount = 4)
		val vm = feed { _, _ -> pageOf(post) }
		vm.refresh()

		vm.toggleLike(post.id)
		vm.toggleLike(post.id)

		assertThat(repo.likes).hasSize(1)
		assertThat(repo.dislikes).hasSize(1)
		assertThat(vm.uiState.value.items.single().liked).isFalse()
		assertThat(vm.uiState.value.items.single().likeCount).isEqualTo(4)
	}

	@Test
	fun likingAPostThatIsNotLoadedDoesNothing() = runTest(mainRule.dispatcher) {
		val vm = feed { _, _ -> pageOf(aFeedItem()) }
		vm.refresh()

		vm.toggleLike(UUID.randomUUID().toString())

		assertThat(repo.likes).isEmpty()
		assertThat(repo.dislikes).isEmpty()
	}

	@Test
	fun aLikeShowsInTheListAndInTheGridAlike() = runTest(mainRule.dispatcher) {
		val post = aFeedItem(liked = false, likeCount = 0)
		val vm = feed { _, _ -> pageOf(post, last = true) }
		vm.refreshAll()

		vm.toggleLike(post.id)

		assertThat(vm.uiState.value.items.single().liked).isTrue()
		assertThat(vm.gridState.value.items.single().liked).isTrue()
		assertThat(vm.gridState.value.items.single().likeCount).isEqualTo(1)
	}
}
