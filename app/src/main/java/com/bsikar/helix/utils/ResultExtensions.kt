package com.bsikar.helix.utils

import com.bsikar.helix.data.model.UiState

fun <T> Result<T>.toUiState(): UiState<T> {
    return fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it) }
    )
}

suspend inline fun <T> safeCall(action: suspend () -> T): Result<T> {
    return try {
        Result.success(action())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend inline fun <T> safeCallWithUiState(action: suspend () -> T): UiState<T> {
    return try {
        UiState.Success(action())
    } catch (e: Exception) {
        UiState.Error(e)
    }
}