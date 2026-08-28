package xyz.stignarnia.uiMovie.sections.collections.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiMovie.sections.collections.details.recycler.MovieDetailsCollectionItem
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsCollectionTranslationsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    suspend fun loadMissingTranslation(
      item: MovieDetailsCollectionItem.MovieItem,
      language: String,
    ) = withContext(dispatchers.IO) {
      try {
        val translation = translationsRepository.loadTranslation(item.movie, language) ?: Translation.EMPTY
        return@withContext item.copy(translation = translation)
      } catch (error: Throwable) {
        Timber.w(error)
        return@withContext item.copy(translation = Translation.EMPTY)
      }
    }
  }
