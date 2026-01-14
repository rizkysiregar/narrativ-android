package com.rizkysiregar.feedzmore.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/01/26
 * Description: Software Engineer
 */


/**
 * An abstract ViewModel that provides common functionality, like accessing the user token.
 * ViewModels that need access to the token can inherit from this class.
 *
 *  Why use abstract? it can be use only to inherit its functionality not create as new instance.
 */
abstract class BaseViewModel(dataStore: DataStoreManager) : ViewModel() {

	/**
	 * A StateFlow that holds the user's authentication token.
	 * It is collected from DataStore and cached in the viewModelScope.
	 */
	val token = dataStore.get(PrefKeys.token, "")
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = ""
		)


	val userId = dataStore.get(PrefKeys.userId, "")
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = ""
		)

}
