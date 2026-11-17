package com.rizkysiregar.narrativ.data.session

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** The push (FCM) token of this device. */
interface PushTokenProvider {
	/** The current token. Throws when it cannot be had (no Play services, no network). */
	suspend fun token(): String

	/** Deletes it, so the next [token] is a new one and nothing sent to the old one reaches this device again. */
	suspend fun delete()
}

@Singleton
class FirebasePushTokenProvider @Inject constructor() : PushTokenProvider {

	override suspend fun token(): String = FirebaseMessaging.getInstance().token.await()

	override suspend fun delete() {
		FirebaseMessaging.getInstance().deleteToken().await()
	}
}
