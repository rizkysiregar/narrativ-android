package com.rizkysiregar.feedzmore.config

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: The values the app reads from Firebase Remote Config. [minSupportedVersionCode] is the lowest
 * versionCode that may still run; 0 means no version is blocked, which is also what an offline first launch gets.
 */
data class AppConfig(
	val minSupportedVersionCode: Long = 0,
	val updateMessage: String = DEFAULT_UPDATE_MESSAGE
) {

	fun requiresUpdate(currentVersionCode: Long): Boolean = currentVersionCode < minSupportedVersionCode

	companion object {
		const val DEFAULT_UPDATE_MESSAGE = "A new version of Feedzmore is available. Please update to keep using the app."
	}
}
