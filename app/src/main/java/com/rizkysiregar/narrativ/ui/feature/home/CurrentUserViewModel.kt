package com.rizkysiregar.narrativ.ui.feature.home

import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The signed-in user's id for the home screens, so opening a user can tell when it is yourself. */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
	dataStore: DataStoreManager
) : BaseViewModel(dataStore)
