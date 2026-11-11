package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** What a row draws of the line that ties a comment to its replies, like the thread line on X. */
internal enum class ThreadLine {
	None,

	/** Top-level comment with replies below it: the line leaves under its avatar. */
	RootStart,

	/** Reply or reply control with more rows below in the same thread: elbow plus a line that goes on. */
	Branch,

	/** Last row of a thread: elbow only. */
	LastBranch
}

/** The thread line runs through the middle of the top-level avatar. */
internal val ThreadLineX = 18.dp

/** Replies and reply controls start here, which is where their avatar (or text) begins. */
internal val ThreadIndent = 48.dp

private val TopLevelAvatarBottom = 6.dp + 36.dp + 4.dp
private val ElbowRadius = 10.dp
private val ElbowGap = 4.dp
private val StrokeWidth = 3.dp

/** Contrasts with the background in both light and dark theme; outlineVariant was too faint. */
@Composable
internal fun threadLineColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

/**
 * Draws [line] behind the row. [elbowY] is the vertical middle of the row's avatar or text, where the
 * elbow turns towards it. Rows must touch each other (no spacing between list items) for the line to
 * look continuous.
 */
internal fun Modifier.threadLine(line: ThreadLine, color: Color, elbowY: Dp): Modifier =
	if (line == ThreadLine.None) this else drawBehind {
		val x = ThreadLineX.toPx()
		val stroke = Stroke(width = StrokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		when (line) {
			ThreadLine.RootStart -> drawLine(
				color = color,
				start = Offset(x, TopLevelAvatarBottom.toPx()),
				end = Offset(x, size.height),
				strokeWidth = stroke.width,
				cap = StrokeCap.Round
			)

			ThreadLine.Branch, ThreadLine.LastBranch -> {
				val cy = elbowY.toPx()
				val r = ElbowRadius.toPx()
				if (line == ThreadLine.Branch) {
					drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke.width, StrokeCap.Round)
				}
				val path = Path().apply {
					moveTo(x, if (line == ThreadLine.Branch) cy - r else 0f)
					if (line == ThreadLine.LastBranch) lineTo(x, cy - r)
					quadraticTo(x, cy, x + r, cy)
					lineTo((ThreadIndent - ElbowGap).toPx(), cy)
				}
				drawPath(path, color, style = stroke)
			}

			ThreadLine.None -> Unit
		}
	}
