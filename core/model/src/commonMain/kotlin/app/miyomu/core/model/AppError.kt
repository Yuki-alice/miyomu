package app.miyomu.core.model

sealed interface AppError {
    val retryable: Boolean

    data class Network(val cause: Throwable? = null) : AppError {
        override val retryable: Boolean = true
    }

    data class Http(val code: Int) : AppError {
        override val retryable: Boolean = code >= 500
    }

    data class Parse(val source: SourceId? = null) : AppError {
        override val retryable: Boolean = false
    }

    data class NotFound(val what: String) : AppError {
        override val retryable: Boolean = false
    }

    data class Storage(val cause: Throwable? = null) : AppError {
        override val retryable: Boolean = false
    }

    data class Unsupported(val feature: String) : AppError {
        override val retryable: Boolean = false
    }
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}
