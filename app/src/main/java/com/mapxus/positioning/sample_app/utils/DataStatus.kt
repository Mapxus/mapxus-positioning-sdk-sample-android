package com.mapxus.common.ui.lib.utils

sealed class DataStatus<out R> {
    data class Success<out T>(val data: T) : DataStatus<T>()
    data class Failed(val errorMessage: String) : DataStatus<Nothing>()
    data object Loading : DataStatus<Nothing>()
    data object Idle : DataStatus<Nothing>()
}