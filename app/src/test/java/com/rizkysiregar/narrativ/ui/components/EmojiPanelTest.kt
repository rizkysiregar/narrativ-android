package com.rizkysiregar.narrativ.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LIMIT = 255

class EmojiPanelTest {

	@Test
	fun insertsAtTheCursorAndMovesItAfterTheEmoji() {
		val result = insertEmoji(TextFieldValue("halo dunia", TextRange(4)), "😂", LIMIT)!!

		assertEquals("halo😂 dunia", result.text)
		assertEquals(TextRange(4 + "😂".length), result.selection)
	}

	@Test
	fun replacesTheSelection() {
		val result = insertEmoji(TextFieldValue("halo dunia", TextRange(5, 10)), "🔥", LIMIT)!!

		assertEquals("halo 🔥", result.text)
	}

	@Test
	fun refusesWhenTheResultWouldPassTheLimit() {
		val full = TextFieldValue("a".repeat(LIMIT - 1), TextRange(LIMIT - 1))

		assertNull(insertEmoji(full, "😂", LIMIT))
	}

	@Test
	fun acceptsAnEmojiThatFillsTheLimitExactly() {
		val almost = TextFieldValue("a".repeat(LIMIT - 2), TextRange(LIMIT - 2))

		assertEquals(LIMIT, insertEmoji(almost, "😂", LIMIT)!!.text.length)
	}
}
