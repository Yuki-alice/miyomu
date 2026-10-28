package app.miyomu.source

open class SourceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class SourceNotSupportedException(message: String) : SourceException(message)

class SourceTimeoutException(message: String, cause: Throwable? = null) : SourceException(message, cause)
