package com.walletscholer.app.domain.result

/**
 * A standardized Result wrapper class to handle success and failure states
 * across all asynchronous database, domain calculations, and network operations.
 */
sealed class AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val code: Int? = null
    ) : AppResult<Nothing>()

    data class ValidationError(
        val field: String,
        val reason: String
    ) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error || this is ValidationError

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Throwable?) -> Unit): AppResult<T> {
        when (this) {
            is Error -> action(message, throwable)
            is ValidationError -> action("Validation failed on $field: $reason", null)
            is Success -> {}
        }
        return this
    }

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, throwable, code)
        is ValidationError -> ValidationError(field, reason)
    }
}
