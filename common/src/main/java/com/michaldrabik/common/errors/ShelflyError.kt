package com.michaldrabik.common.errors

sealed class ShelflyError(
  errorMessage: String?,
) : Throwable(errorMessage) {

  object ValidationError : ShelflyError("ValidationError")

  object ResourceConflictError : ShelflyError("ResourceConflictError")

  object ResourceNotFoundError : ShelflyError("ResourceNotFoundError")

  object AccountLockedError : ShelflyError("AccountLockedError")

  object AccountLimitsError : ShelflyError("AccountLimitsError")

  data class UnauthorizedError(
    val errorMessage: String?,
  ) : ShelflyError(errorMessage)

  data class UnknownHttpError(
    val errorMessage: String?,
  ) : ShelflyError(errorMessage)

  data class UnknownError(
    val errorMessage: String?,
  ) : ShelflyError(errorMessage)

  object CoroutineCancellation : ShelflyError("")
}
