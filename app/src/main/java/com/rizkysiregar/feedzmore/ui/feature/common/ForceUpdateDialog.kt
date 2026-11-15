package com.rizkysiregar.feedzmore.ui.feature.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Blocks the app until the user installs a newer version. It cannot be dismissed (back and a
 * tap outside do nothing); the only way out is the Play Store.
 */
@Composable
fun ForceUpdateDialog(message: String) {
	val context = LocalContext.current
	Dialog(
		onDismissRequest = {},
		properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
	) {
		Surface(
			shape = RoundedCornerShape(24.dp),
			tonalElevation = 3.dp,
			color = MaterialTheme.colorScheme.surface
		) {
			Column(
				modifier = Modifier
					.padding(24.dp)
					.widthIn(min = 260.dp, max = 320.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Box(
					modifier = Modifier
						.size(56.dp)
						.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
					contentAlignment = Alignment.Center
				) {
					Icon(
						imageVector = Icons.Outlined.SystemUpdate,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.size(28.dp)
					)
				}

				Spacer(modifier = Modifier.height(16.dp))

				Text(
					text = "Update required",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = message,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center,
					modifier = Modifier
						.heightIn(max = 220.dp)
						.verticalScroll(rememberScrollState())
				)

				Spacer(modifier = Modifier.height(24.dp))

				Button(
					onClick = { openPlayStore(context) },
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp),
					shape = RoundedCornerShape(12.dp)
				) {
					Text("Update")
				}
			}
		}
	}
}

private fun openPlayStore(context: Context) {
	val packageName = context.packageName
	val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
	try {
		context.startActivity(market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
	} catch (_: ActivityNotFoundException) {
		// no Play Store app on this device
		val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
		context.startActivity(web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
	}
}

@Composable
@Preview(showBackground = true)
fun ForceUpdateDialogPreview() {
	FeedzmoreTheme {
		ForceUpdateDialog(message = "A new version of Feedzmore is available. Please update to keep using the app.")
	}
}
