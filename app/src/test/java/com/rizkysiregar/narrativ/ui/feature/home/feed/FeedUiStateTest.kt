package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.testing.aFeedItem
import org.junit.Test

class FeedUiStateTest {

	@Test
	fun theSkeletonShowsWhileTheFirstPageIsOnItsWay() {
		assertThat(FeedUiState().showsSkeleton).isTrue() // before the first request even started
		assertThat(FeedUiState(isRefreshing = true).showsSkeleton).isTrue() // what refresh() starts from
	}

	@Test
	fun aFeedThatLoadedAndIsEmptyShowsItsOwnMessageNotTheSkeleton() {
		assertThat(FeedUiState(hasLoadedInitialPage = true).showsSkeleton).isFalse()
	}

	@Test
	fun aFirstPageThatFailedShowsTheErrorNotTheSkeleton() {
		assertThat(FeedUiState(isError = true, errorMessage = "Gagal memuat").showsSkeleton).isFalse()
		assertThat(FeedUiState(errorMessage = "Gagal memuat").showsSkeleton).isFalse()
	}

	@Test
	fun postsOnTheScreenAreNeverHiddenBehindTheSkeleton() {
		val loaded = FeedUiState(items = listOf(aFeedItem()), hasLoadedInitialPage = true)

		assertThat(loaded.showsSkeleton).isFalse()
		assertThat(loaded.copy(isRefreshing = true).showsSkeleton).isFalse()
		assertThat(loaded.copy(isLoadingMore = true).showsSkeleton).isFalse()
	}
}
