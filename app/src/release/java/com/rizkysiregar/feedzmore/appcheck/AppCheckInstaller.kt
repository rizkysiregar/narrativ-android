package com.rizkysiregar.feedzmore.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Release builds prove themselves with Play Integrity: an app installed from Google Play,
 * on a device Google trusts. The debug source set has its own copy that uses the debug provider.
 */
object AppCheckInstaller {
	fun install() {
		FirebaseAppCheck.getInstance()
			.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
	}
}
