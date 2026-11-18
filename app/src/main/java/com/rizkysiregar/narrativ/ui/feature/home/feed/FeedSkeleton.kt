package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.components.rememberSkeletonPhase
import com.rizkysiregar.narrativ.ui.components.skeleton

private const val SKELETON_CARDS = 2

/**
 * What the feed shows while its first page is on its way: a couple of cards with the outline of a post, so the screen
 * is not left blank. Two are enough to fill the screen, the list is not scrollable while they are shown.
 */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
	val phase = rememberSkeletonPhase()
	val loading = stringResource(R.string.home_loading)
	Column(
		modifier = modifier
			.fillMaxWidth()
			// one "loading" for a screen reader instead of every block of every card
			.clearAndSetSemantics { contentDescription = loading },
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		repeat(SKELETON_CARDS) { FeedItemSkeleton(phase) }
	}
}

/** The outline of a [FeedItemCard]: the same card, the same paddings, blocks where the text and the media go. */
@Composable
private fun FeedItemSkeleton(phase: State<Float>) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
	) {
		Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 16.dp)) {
			// author
			Row(verticalAlignment = Alignment.CenterVertically) {
				Spacer(Modifier.size(38.dp).skeleton(phase, CircleShape))
				Spacer(Modifier.width(10.dp))
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					Spacer(Modifier.width(110.dp).height(12.dp).skeleton(phase))
					Spacer(Modifier.width(64.dp).height(10.dp).skeleton(phase))
				}
			}
			// caption
			Spacer(Modifier.height(14.dp))
			Spacer(Modifier.fillMaxWidth().height(12.dp).skeleton(phase))
			Spacer(Modifier.height(8.dp))
			Spacer(Modifier.fillMaxWidth(0.62f).height(12.dp).skeleton(phase))
			// media
			Spacer(Modifier.height(14.dp))
			Spacer(Modifier.fillMaxWidth().aspectRatio(1f).skeleton(phase, RoundedCornerShape(16.dp)))
			// actions
			Spacer(Modifier.height(14.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
				Spacer(Modifier.width(44.dp).height(14.dp).skeleton(phase))
				Spacer(Modifier.width(44.dp).height(14.dp).skeleton(phase))
			}
		}
	}
}
