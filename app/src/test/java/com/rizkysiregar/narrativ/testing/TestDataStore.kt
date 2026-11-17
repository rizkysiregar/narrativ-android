package com.rizkysiregar.narrativ.testing

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import org.junit.rules.TemporaryFolder
import java.io.File

/** A [DataStoreManager] on a real DataStore file; its behaviour is simple enough that a fake would only copy it. */
fun testDataStoreManager(scope: CoroutineScope, file: File): DataStoreManager =
	DataStoreManager(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))

/** A not yet created file path DataStore accepts: it insists on the .preferences_pb extension. */
fun TemporaryFolder.preferencesFile(name: String = "test"): File = File(root, "$name.preferences_pb")
