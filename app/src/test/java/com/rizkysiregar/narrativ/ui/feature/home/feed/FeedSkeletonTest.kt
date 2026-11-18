package com.rizkysiregar.narrativ.ui.feature.home.feed

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.rizkysiregar.narrativ.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class FeedSkeletonTest {

	@get:Rule
	val compose = createComposeRule()

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun aScreenReaderHearsOneLoadingInsteadOfEveryBlock() {
		compose.setContent { FeedSkeleton() }

		compose.onNodeWithContentDescription(context.getString(R.string.home_loading)).assertIsDisplayed()
	}
}
