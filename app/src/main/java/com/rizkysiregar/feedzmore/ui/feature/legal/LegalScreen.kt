package com.rizkysiregar.feedzmore.ui.feature.legal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.rizkysiregar.feedzmore.ui.theme.BgBase

/**
 * A legal page inside the app. The WebView is locked down: no JavaScript (the pages are static), no file or
 * content access, nothing but our own `legal/` pages may load (any other link is ignored), and no session
 * token is ever sent, since the pages are public. An email link opens the mail app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
	document: LegalDocument,
	onBackClick: () -> Unit
) {
	val language = remember { LegalDocument.deviceLanguage() }
	val url = remember(document, language) { document.url(language) }
	var webView by remember { mutableStateOf<WebView?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var hasFailed by remember { mutableStateOf(false) }
	var canGoBack by remember { mutableStateOf(false) }

	// back walks through the pages opened from inside the document before leaving the screen
	BackHandler(enabled = canGoBack) { webView?.goBack() }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(document.title(language)) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = if (language == LegalDocument.LANGUAGE_ID) "Kembali" else "Back"
						)
					}
				}
			)
		}
	) { padding ->
		Box(modifier = Modifier.fillMaxSize().padding(padding)) {
			AndroidView(
				modifier = Modifier.fillMaxSize(),
				factory = { context ->
					WebView(context).apply {
						layoutParams = ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						)
						// the page is dark, and so is this background, so nothing flashes white while it loads
						setBackgroundColor(BgBase.toArgb())
						lockDown(settings)
						webViewClient = LegalWebViewClient(
							context = context,
							onStarted = {
								isLoading = true
								hasFailed = false
							},
							onFinished = { view ->
								isLoading = false
								canGoBack = view.canGoBack()
							},
							onFailed = {
								isLoading = false
								hasFailed = true
							}
						)
						loadUrl(url)
						webView = this
					}
				},
				onRelease = { view ->
					view.stopLoading()
					view.destroy()
					if (webView === view) webView = null
				}
			)

			if (isLoading && !hasFailed) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}

			if (hasFailed) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(24.dp)
						.padding(top = 0.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = if (language == LegalDocument.LANGUAGE_ID) {
							"Halaman tidak bisa dimuat. Periksa koneksi Anda."
						} else {
							"The page could not be loaded. Check your connection."
						},
						textAlign = TextAlign.Center,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					TextButton(onClick = {
						hasFailed = false
						webView?.loadUrl(url)
					}) { Text(if (language == LegalDocument.LANGUAGE_ID) "Coba lagi" else "Try again") }
				}
			}
		}
	}
}

private fun lockDown(settings: WebSettings) {
	settings.javaScriptEnabled = false
	settings.domStorageEnabled = false
	settings.allowFileAccess = false
	settings.allowContentAccess = false
	settings.javaScriptCanOpenWindowsAutomatically = false
	settings.setSupportMultipleWindows(false)
	settings.setGeolocationEnabled(false)
	settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
	settings.safeBrowsingEnabled = true
	settings.saveFormData = false
}

private class LegalWebViewClient(
	private val context: Context,
	private val onStarted: () -> Unit,
	private val onFinished: (WebView) -> Unit,
	private val onFailed: () -> Unit
) : WebViewClient() {

	// returning true means "handled": the WebView does not navigate
	override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
		val target = request.url
		return when {
			LegalDocument.isOwnPage(target.toString()) -> false
			target.scheme == "mailto" -> {
				openMailApp(target)
				true
			}
			else -> true
		}
	}

	override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = onStarted()

	override fun onPageFinished(view: WebView, url: String?) = onFinished(view)

	override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
		if (request.isForMainFrame) onFailed()
	}

	override fun onReceivedHttpError(
		view: WebView,
		request: WebResourceRequest,
		errorResponse: android.webkit.WebResourceResponse
	) {
		if (request.isForMainFrame) onFailed()
	}

	private fun openMailApp(mailto: android.net.Uri) {
		try {
			context.startActivity(
				Intent(Intent.ACTION_SENDTO, "mailto:${mailto.schemeSpecificPart.substringBefore('?')}".toUri())
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)
		} catch (_: ActivityNotFoundException) {
			// no mail app installed: the address stays readable on the page
		}
	}
}
