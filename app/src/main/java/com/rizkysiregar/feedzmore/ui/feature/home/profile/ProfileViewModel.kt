package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */


@HiltViewModel
class ProfileViewModel @Inject constructor(
	private val dataStore: DataStoreManager
) : ViewModel() {
	fun clearDataStore() {
		viewModelScope.launch {
			dataStore.clearAll()
		}
	}
}