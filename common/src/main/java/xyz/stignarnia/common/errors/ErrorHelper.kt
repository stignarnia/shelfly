package xyz.stignarnia.common.errors

import xyz.stignarnia.common.errors.ShelflyError.AccountLimitsError
import xyz.stignarnia.common.errors.ShelflyError.AccountLockedError
import xyz.stignarnia.common.errors.ShelflyError.CoroutineCancellation
import xyz.stignarnia.common.errors.ShelflyError.ResourceConflictError
import xyz.stignarnia.common.errors.ShelflyError.ResourceNotFoundError
import xyz.stignarnia.common.errors.ShelflyError.UnauthorizedError
import xyz.stignarnia.common.errors.ShelflyError.UnknownError
import xyz.stignarnia.common.errors.ShelflyError.UnknownHttpError
import xyz.stignarnia.common.errors.ShelflyError.ValidationError
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
