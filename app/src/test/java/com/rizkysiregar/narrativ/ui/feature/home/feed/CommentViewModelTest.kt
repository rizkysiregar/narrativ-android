package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.domain.feed.AddCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.DeleteCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.GetCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.GetRepliesUseCase
import com.rizkysiregar.narrativ.domain.feed.PinCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.UnpinCommentUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.aComment
import com.rizkysiregar.narrativ.testing.aPage
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeFeedRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import com.rizkysiregar.narrativ.testing.uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val repo = FakeFeedRepository()
	private val analytics = FakeAppAnalytics()
	private val strings = FakeAppStrings()
	private val events = CopyOnWriteArrayList<CommentEvent>()
	private val postId = uuid()

	/** What the server currently holds, so a refresh after a send sees the new comment. */
	private var serverComments = listOf<CommentItem>()
	private var serverReplies = mapOf<String, List<CommentItem>>()

	private suspend fun TestScope.newViewModel(): CommentViewModel {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		dataStore.put(PrefKeys.username, "rizky")
		repo.onGetComment = { successOf(aPage(serverComments, last = true)) }
		repo.onGetReplies = { r -> serverReplies[r.commentId.toString()].orEmpty().let { successOf(aPage(it, last = true)) } }
		val vm = CommentViewModel(
			GetCommentUseCase(repo), GetRepliesUseCase(repo), AddCommentUseCase(repo), DeleteCommentUseCase(repo),
			PinCommentUseCase(repo), UnpinCommentUseCase(repo), dataStore, analytics, strings
		)
		backgroundScope.launch(mainRule.dispatcher) { vm.events.collect { events += it } }
		return vm
	}

	private fun <T> pending(deferred: CompletableDeferred<ResultState<T>>): Flow<ResultState<T>> = flow {
		emit(ResultState.Loading)
		emit(deferred.await())
	}

	private fun comment(id: String = uuid(), content: String = "isi", parentId: String? = null, replyCount: Int = 0) =
		aComment(id = id, content = content, parentId = parentId, replyCount = replyCount)

	// --- loading ---

	@Test
	fun theFirstPageLoads() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		serverComments = listOf(comment(), comment())

		vm.loadComments(postId)

		assertThat(vm.comments.value).isEqualTo(serverComments)
		assertThat(vm.hasMore.value).isFalse()
		assertThat(vm.isLoading.value).isFalse()
	}

	@Test
	fun moreCommentsAreAppendedUntilTheLastPage() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val first = listOf(comment())
		val second = listOf(comment())
		repo.onGetComment = { r -> successOf(if (r.page == 0) aPage(first, last = false) else aPage(second, last = true)) }
		vm.loadComments(postId)
		assertThat(vm.hasMore.value).isTrue()

		vm.loadMore()
		vm.loadMore() // nothing left, no request

		assertThat(vm.comments.value).isEqualTo(first + second)
		assertThat(vm.hasMore.value).isFalse()
		assertThat(repo.commentRequests.map { it.page }).containsExactly(0, 1).inOrder()
	}

	@Test
	fun scrollingTwiceWhileLoadingAsksOnce() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val slow = CompletableDeferred<ResultState<PaginationResponseWrapper<CommentItem>>>()
		repo.onGetComment = { r -> if (r.page == 0) successOf(aPage(listOf(comment()), last = false)) else pending(slow) }
		vm.loadComments(postId)

		vm.loadMore()
		vm.loadMore()

		assertThat(repo.commentRequests.map { it.page }).containsExactly(0, 1).inOrder()
		assertThat(vm.isLoading.value).isTrue()
	}

	@Test
	fun aFailedLoadShowsTheErrorAndCanBeRetried() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		var fail = true
		serverComments = listOf(comment())
		repo.onGetComment = { if (fail) failureOf("Gagal") else successOf(aPage(serverComments, last = true)) }

		vm.loadComments(postId)
		assertThat(vm.errorMessage.value).isEqualTo("Gagal")
		assertThat(vm.isLoading.value).isFalse()

		fail = false
		vm.loadMore()
		assertThat(vm.errorMessage.value).isNull()
		assertThat(vm.comments.value).isEqualTo(serverComments)
	}

	@Test
	fun anAnswerForThePreviousPostIsThrownAway() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val slowOld = CompletableDeferred<ResultState<PaginationResponseWrapper<CommentItem>>>()
		val other = uuid()
		val current = listOf(comment(content = "post baru"))
		repo.onGetComment = { r -> if (r.postId == UUID.fromString(postId)) pending(slowOld) else successOf(aPage(current, last = true)) }

		vm.loadComments(postId)
		vm.loadComments(other)
		slowOld.complete(ResultState.Success(aPage(listOf(comment(content = "post lama")), last = true)))

		assertThat(vm.comments.value).isEqualTo(current)
	}

	// --- replies ---

	@Test
	fun openingAThreadLoadsItsRepliesAndClosingItDropsThem() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment(replyCount = 0)
		val reply = comment(parentId = root.id)
		serverComments = listOf(root)
		serverReplies = mapOf(root.id to listOf(reply))
		vm.loadComments(postId)

		vm.toggleReplies(root.id)
		assertThat(vm.replies.value[root.id]!!.items).containsExactly(reply)
		assertThat(vm.comments.value.single().replyCount).isEqualTo(1) // synced from the server total

		vm.toggleReplies(root.id)
		assertThat(vm.replies.value).doesNotContainKey(root.id)
	}

	// --- sending ---

	@Test
	fun aSentCommentShowsAsPendingThenIsReplacedByTheServerList() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val existing = comment(content = "lama")
		serverComments = listOf(existing)
		vm.loadComments(postId)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onAddComment = { pending(answer) }

		vm.addComment(postId, "  halo dunia  ")

		val shown = vm.comments.value.last()
		assertThat(shown.isPending).isTrue()
		assertThat(shown.commentContent).isEqualTo("halo dunia") // trimmed
		assertThat(shown.username).isEqualTo("rizky")
		assertThat(vm.isSubmitting.value).isTrue()
		assertThat(events.first()).isEqualTo(CommentEvent.Sent)

		val saved = comment(content = "halo dunia")
		serverComments = listOf(existing, saved)
		answer.complete(ResultState.Success("ok"))

		// the saved comment keeps the key of the row it was sending in, otherwise it is the server's
		assertThat(vm.comments.value.map { it.copy(localKey = null) }).isEqualTo(listOf(existing, saved))
		assertThat(vm.comments.value.none { it.isPending }).isTrue()
		assertThat(vm.isSubmitting.value).isFalse()
		assertThat(events.last()).isEqualTo(CommentEvent.ScrollToBottom)
		assertThat(events.filterIsInstance<CommentEvent.SendFailed>()).isEmpty()
		assertThat(analytics.events.single().name).isEqualTo(AnalyticsEvents.COMMENT_ADDED)
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.IS_REPLY]).isEqualTo(false)
		assertThat(repo.addedComments.single().content).isEqualTo("halo dunia")
		assertThat(repo.addedComments.single().parentId).isNull()
	}

	@Test
	fun aBlankCommentIsNotSentAndASecondOneWaitsForTheFirst() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		vm.loadComments(postId)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onAddComment = { pending(answer) }

		vm.addComment(postId, "   ")
		assertThat(repo.addedComments).isEmpty()

		vm.addComment(postId, "satu")
		vm.addComment(postId, "dua")
		assertThat(repo.addedComments.map { it.content }).containsExactly("satu")
	}

	@Test
	fun aFailedCommentIsRemovedAndTheTextComesBack() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		serverComments = listOf(comment())
		vm.loadComments(postId)
		repo.onAddComment = { failureOf("Server sibuk") }

		vm.addComment(postId, "jangan hilang")

		assertThat(vm.comments.value).isEqualTo(serverComments) // the pending row is gone
		assertThat(vm.isSubmitting.value).isFalse()
		assertThat(events).contains(CommentEvent.SendFailed("jangan hilang"))
		assertThat(events).contains(CommentEvent.ShowMessage("Server sibuk"))
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.COMMENT_FAILED)
	}

	// --- replying ---

	@Test
	fun aReplyIsSentToTheCommentAndItsThreadIsRefreshed() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		val saved = comment(content = "balasan", parentId = root.id)
		serverComments = listOf(root)
		vm.loadComments(postId)
		repo.onAddComment = { successOf("ok") }
		serverReplies = mapOf(root.id to listOf(saved))

		vm.startReply(root)
		vm.addComment(postId, "balasan")

		assertThat(repo.addedComments.single().parentId).isEqualTo(UUID.fromString(root.id))
		assertThat(vm.replyTarget.value).isNull()
		assertThat(vm.replies.value[root.id]!!.items.map { it.copy(localKey = null) }).containsExactly(saved)
		assertThat(events.last()).isEqualTo(CommentEvent.ScrollToLastReply(root.id))
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.IS_REPLY]).isEqualTo(true)
	}

	// --- the row of a sent comment keeps its key, so the list updates it in place instead of overlapping two rows ---

	@Test
	fun aSentReplyKeepsTheRowItWasShownUnderOnceSaved() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		serverComments = listOf(root)
		vm.loadComments(postId)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onAddComment = { pending(answer) }
		val older = comment(content = "lama", parentId = root.id)
		val saved = comment(content = "balasan", parentId = root.id)
		serverReplies = mapOf(root.id to listOf(older, saved))

		vm.startReply(root)
		vm.addComment(postId, "balasan")
		val keyWhileSending = vm.replies.value[root.id]!!.items.single().listKey
		answer.complete(ResultState.Success("ok"))

		val items = vm.replies.value[root.id]!!.items
		assertThat(items.map { it.id }).containsExactly(older.id, saved.id).inOrder()
		assertThat(items.last().listKey).isEqualTo(keyWhileSending)
		assertThat(items.first().listKey).isEqualTo(older.id)
	}

	@Test
	fun theKeyOutlivesTheNextRefresh() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		serverComments = listOf(root)
		vm.loadComments(postId)
		repo.onAddComment = { successOf("ok") }
		val first = comment(content = "satu", parentId = root.id)
		serverReplies = mapOf(root.id to listOf(first))
		vm.startReply(root)
		vm.addComment(postId, "satu")
		val firstKey = vm.replies.value[root.id]!!.items.single().listKey

		val second = comment(content = "dua", parentId = root.id)
		serverReplies = mapOf(root.id to listOf(first, second))
		vm.startReply(root)
		vm.addComment(postId, "dua")

		val items = vm.replies.value[root.id]!!.items
		assertThat(items.first().listKey).isEqualTo(firstKey)
		assertThat(items.map { it.listKey }.toSet()).hasSize(2)
	}

	@Test
	fun aSentTopLevelCommentKeepsItsRowToo() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val existing = comment()
		serverComments = listOf(existing)
		vm.loadComments(postId)
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onAddComment = { pending(answer) }
		val saved = comment(content = "halo")
		serverComments = listOf(existing, saved)

		vm.addComment(postId, "halo")
		val keyWhileSending = vm.comments.value.last().listKey
		answer.complete(ResultState.Success("ok"))

		assertThat(vm.comments.value.last().id).isEqualTo(saved.id)
		assertThat(vm.comments.value.last().listKey).isEqualTo(keyWhileSending)
	}

	@Test
	fun aFailedReplyKeepsTheTargetAndClosesAThreadItOpened() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		serverComments = listOf(root)
		vm.loadComments(postId)
		repo.onAddComment = { failureOf("Gagal") }

		vm.startReply(root)
		vm.addComment(postId, "balasan")

		assertThat(vm.replyTarget.value).isEqualTo(root)
		assertThat(vm.replies.value).doesNotContainKey(root.id) // it was collapsed, so it goes back to collapsed
		assertThat(events).contains(CommentEvent.SendFailed("balasan"))
	}

	@Test
	fun aTakenDownCommentCannotBeRepliedTo() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()

		vm.startReply(comment().copy(moderationStatus = CommentItem.MODERATION_TAKEN_DOWN))

		assertThat(vm.replyTarget.value).isNull()
	}

	// --- quoting ---

	@Test
	fun aQuoteGoesWithTheCommentAndIsClearedOnceSent() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		vm.loadComments(postId)
		repo.onAddComment = { successOf("ok") }

		vm.startQuote("  hujan turun pelan ")
		vm.addComment(postId, "indah")

		assertThat(repo.addedComments.single().quoteText).isEqualTo("hujan turun pelan")
		assertThat(repo.addedComments.single().parentId).isNull()
		assertThat(vm.quote.value).isNull()
	}

	@Test
	fun aFailedSendPutsTheQuoteBack() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		vm.loadComments(postId)
		repo.onAddComment = { failureOf("Gagal") }

		vm.startQuote("hujan")
		vm.addComment(postId, "indah")

		assertThat(vm.quote.value).isEqualTo("hujan")
	}

	@Test
	fun aReplyAndAQuoteExcludeEachOther() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()

		vm.startQuote("hujan")
		vm.startReply(root)
		assertThat(vm.quote.value).isNull()

		vm.startQuote("hujan")
		assertThat(vm.replyTarget.value).isNull()
	}

	// --- pinning ---

	@Test
	fun pinningMovesTheCommentOnTopAtOnce() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val first = comment().copy(createdAt = "2026-11-19T01:00:00Z")
		val second = comment().copy(createdAt = "2026-11-19T02:00:00Z")
		serverComments = listOf(first, second)
		vm.loadComments(postId)
		repo.onPinComment = { _, _ -> successOf("ok") }

		vm.togglePin(postId, second)

		assertThat(vm.comments.value.map { it.id }).containsExactly(second.id, first.id).inOrder()
		assertThat(vm.comments.value.first().pinned).isTrue()
	}

	@Test
	fun unpinningPutsTheCommentBackInItsPlace() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val first = comment().copy(createdAt = "2026-11-19T01:00:00Z")
		val second = comment().copy(createdAt = "2026-11-19T02:00:00Z", pinned = true)
		serverComments = listOf(second, first)
		vm.loadComments(postId)
		repo.onUnpinComment = { successOf("ok") }

		vm.togglePin(postId, second)

		assertThat(vm.comments.value.map { it.id }).containsExactly(first.id, second.id).inOrder()
		assertThat(vm.comments.value.none { it.pinned }).isTrue()
	}

	@Test
	fun aRefusedPinGoesBack() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val first = comment()
		val second = comment()
		serverComments = listOf(first, second)
		vm.loadComments(postId)
		repo.onPinComment = { _, _ -> failureOf("Tidak boleh") }

		vm.togglePin(postId, second)

		assertThat(vm.comments.value).isEqualTo(serverComments)
		assertThat(events).contains(CommentEvent.ShowMessage("Tidak boleh"))
	}

	@Test
	fun aReplyCannotBePinned() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()

		vm.togglePin(postId, comment(parentId = uuid()))

		assertThat(vm.comments.value).isEmpty()
	}

	// --- deleting ---

	@Test
	fun deletingATopLevelCommentRemovesItsThreadAndTheReplyTarget() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		val keep = comment()
		serverComments = listOf(root, keep)
		serverReplies = mapOf(root.id to listOf(comment(parentId = root.id)))
		vm.loadComments(postId)
		vm.toggleReplies(root.id)
		vm.startReply(root)
		repo.onDeleteComment = { successOf("deleted") }

		vm.deleteComment(root)

		assertThat(vm.comments.value).containsExactly(keep)
		assertThat(vm.replies.value).doesNotContainKey(root.id)
		assertThat(vm.replyTarget.value).isNull()
		assertThat(events.last()).isEqualTo(CommentEvent.ShowMessage(strings.get(R.string.comment_deleted)))
	}

	@Test
	fun deletingAReplyUpdatesTheCountAndClearsTheTagsOfItsAnswers() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment(replyCount = 2)
		val first = comment(parentId = root.id)
		val answer = comment(parentId = root.id).copy(replyToCommentId = first.id, replyToUsername = first.username)
		serverComments = listOf(root)
		serverReplies = mapOf(root.id to listOf(first, answer))
		vm.loadComments(postId)
		vm.toggleReplies(root.id)
		repo.onDeleteComment = { successOf("deleted") }

		vm.deleteComment(first)

		val items = vm.replies.value[root.id]!!.items
		assertThat(items).hasSize(1)
		assertThat(items.single().replyToCommentId).isNull() // the server clears the tag, so does the app
		assertThat(items.single().replyToUsername).isNull()
		assertThat(vm.comments.value.single().replyCount).isEqualTo(1)
		assertThat(events.last()).isEqualTo(CommentEvent.ShowMessage(strings.get(R.string.reply_deleted)))
	}

	@Test
	fun aFailedDeleteKeepsTheCommentAndSaysWhy() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		serverComments = listOf(root)
		vm.loadComments(postId)
		repo.onDeleteComment = { failureOf("Tidak boleh") }

		vm.deleteComment(root)

		assertThat(vm.comments.value).containsExactly(root)
		assertThat(events.last()).isEqualTo(CommentEvent.ShowMessage("Tidak boleh"))
	}

	@Test
	fun aPendingCommentCannotBeDeletedAndADoubleDeleteIsOneRequest() = runTest(mainRule.dispatcher) {
		val vm = newViewModel()
		val root = comment()
		serverComments = listOf(root)
		vm.loadComments(postId)
		var requests = 0
		val answer = CompletableDeferred<ResultState<String>>()
		repo.onDeleteComment = { requests++; pending(answer) }

		vm.deleteComment(root.copy(isPending = true))
		assertThat(requests).isEqualTo(0)

		vm.deleteComment(root)
		vm.deleteComment(root)
		assertThat(requests).isEqualTo(1)
	}
}
