package com.bsikar.helix.data.model

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
    
    val isLoading: Boolean
        get() = this is Loading
    
    val isSuccess: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun getErrorOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}

inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

inline fun <T> UiState<T>.onError(action: (Throwable) -> Unit): UiState<T> {
    if (this is UiState.Error) action(exception)
    return this
}

fun <T> Result<T>.toUiState(): UiState<T> {
    return fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it) }
    )
}