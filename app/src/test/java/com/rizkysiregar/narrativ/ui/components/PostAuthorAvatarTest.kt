package com.rizkysiregar.narrativ.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rizkysiregar.narrativ.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PostAuthorAvatarTest {

	@get:Rule
	val compose = createComposeRule()

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val photoDescription get() = context.getString(R.string.cd_profile_photo)

	@Test
	fun anAuthorWithoutAPhotoShowsTheirInitialsAsBefore() {
		compose.setContent { PostAuthorAvatar(imageUrl = null, name = "Rizky Siregar", size = 40.dp) }

		compose.onNodeWithText("RS").assertIsDisplayed()
		compose.onNodeWithContentDescription(photoDescription).assertDoesNotExist()
	}

	@Test
	fun aBlankPhotoUrlCountsAsNoPhoto() {
		compose.setContent { PostAuthorAvatar(imageUrl = "  ", name = "Rizky Siregar", size = 40.dp) }

		compose.onNodeWithText("RS").assertIsDisplayed()
		compose.onNodeWithContentDescription(photoDescription).assertDoesNotExist()
	}

	@Test
	fun anAuthorWithAPhotoGetsItOverTheirInitialsWhichStayUnderIt() {
		// the address does not exist, so the photo never arrives: what is left to see is the initials, not a hole
		compose.setContent { PostAuthorAvatar(imageUrl = "https://photo.invalid/avatar.jpg", name = "Rizky Siregar", size = 40.dp) }

		compose.onNodeWithContentDescription(photoDescription).assertExists()
		compose.onNodeWithText("RS").assertExists()
	}
}
