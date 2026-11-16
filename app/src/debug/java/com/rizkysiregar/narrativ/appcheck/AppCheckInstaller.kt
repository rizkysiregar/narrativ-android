package com.rizkysiregar.narrativ.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Debug builds, emulators and anything installed outside Google Play cannot pass Play
 * Integrity. The debug provider prints a secret to logcat on first use (filter "DebugAppCheckProvider");
 * add it under Firebase Console > App Check > Manage debug tokens to get valid tokens on that device.
 */
object AppCheckInstaller {
	fun install() {
		FirebaseAppCheck.getInstance()
			.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
	}
}
