package com.michaldrabik.common.errors

import com.michaldrabik.common.errors.ShelflyError.AccountLimitsError
import com.michaldrabik.common.errors.ShelflyError.AccountLockedError
import com.michaldrabik.common.errors.ShelflyError.CoroutineCancellation
import com.michaldrabik.common.errors.ShelflyError.ResourceConflictError
import com.michaldrabik.common.errors.ShelflyError.ResourceNotFoundError
import com.michaldrabik.common.errors.ShelflyError.UnauthorizedError
import com.michaldrabik.common.errors.ShelflyError.UnknownError
import com.michaldrabik.common.errors.ShelflyError.UnknownHttpError
import com.michaldrabik.common.errors.ShelflyError.ValidationError
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException

object ErrorHelper {

  fun parse(error: Throwable): ShelflyError =
    when (error) {
      is ShelflyError -> {
        error
      }
      is HttpException -> {
        when (error.code()) {
          in arrayOf(401, 403) -> UnauthorizedError(error.message)
          404 -> ResourceNotFoundError
          409 -> ResourceConflictError
          420 -> AccountLimitsError
          422 -> ValidationError
          423 -> AccountLockedError
          else -> UnknownHttpError(error.message)
        }
      }
      is CancellationException -> {
        CoroutineCancellation
      }
      else -> {
        UnknownError(error.message)
      }
    }
}
