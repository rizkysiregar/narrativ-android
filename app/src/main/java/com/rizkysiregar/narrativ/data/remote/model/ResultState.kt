package com.rizkysiregar.narrativ.data.remote.model


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

sealed class ResultState<out T> {
	data class Success<out T>(val data: T) : ResultState<T>()
	data class Failure(val message: String, val throwable: Throwable? = null) : ResultState<Nothing>()
	object Loading : ResultState<Nothing>()
	object Idle : ResultState<Nothing>()
}