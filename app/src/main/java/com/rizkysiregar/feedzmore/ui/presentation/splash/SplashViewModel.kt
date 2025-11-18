package com.rizkysiregar.feedzmore.ui.presentation.splash

import androidx.lifecycle.ViewModel
import com.rizkysiregar.feedzmore.data.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */

@HiltViewModel
class SplashViewModel constructor(
	private val dataStore: DataStoreManager
) : ViewModel() {

}