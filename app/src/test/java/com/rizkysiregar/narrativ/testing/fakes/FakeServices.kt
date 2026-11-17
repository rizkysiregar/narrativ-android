package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.data.session.GoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.RemoteSessionCleaner

class FakeDeviceSyncScheduler : DeviceSyncScheduler {
	val scheduled = mutableListOf<Boolean>()

	override fun schedule(force: Boolean) {
		scheduled += force
	}
}

class FakeRemoteSessionCleaner : RemoteSessionCleaner {
	val calls = mutableListOf<String>()

	/** Runs inside the call, so a test can look at what state the app is in at that moment. */
	var onSignOut: suspend () -> Unit = {}

	override suspend fun signOut() {
		calls += "signOut"
		onSignOut()
	}

	override suspend fun dropPushToken() {
		calls += "dropPushToken"
	}
}

class FakeGoogleIdTokenProvider : GoogleIdTokenProvider {
	var onGet: () -> String = { "google-id-token" }

	override suspend fun get(): String = onGet()
}

class FakePushTokenProvider : com.rizkysiregar.narrativ.data.session.PushTokenProvider {
	var onToken: suspend () -> String = { "fcm-token-1" }
	var tokenCalls = 0
	var deleteCalls = 0

	override suspend fun token(): String {
		tokenCalls++
		return onToken()
	}

	override suspend fun delete() {
		deleteCalls++
	}
}

class FakeDeviceRegistrar : com.rizkysiregar.narrativ.data.repository.DeviceRegistrar {
	var result = true
	val calls = mutableListOf<Boolean>()

	override suspend fun sync(force: Boolean): Boolean {
		calls += force
		return result
	}
}
