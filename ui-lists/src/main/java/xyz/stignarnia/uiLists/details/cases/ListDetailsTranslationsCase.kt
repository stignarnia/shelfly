package xyz.stignarnia.uiLists.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiLists.details.recycler.ListDetailsItem
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject

@ViewModelScoped
class ListDetailsTranslationsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val translationsRepository: TranslationsRepository,
  ) {
    fun getLanguage() = translationsRepository.getLanguage()

    suspend fun loadTranslation(
      item: ListDetailsItem,
      onlyLocal: Boolean,
    ): Translation? =
      withContext(dispatchers.IO) {
        val language = getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        when {
          item.isShow() -> {
            translationsRepository.loadTranslation(
              show = item.requireShow(),
              language = language,
              onlyLocal = onlyLocal,
            )
          }

          item.isMovie() -> {
            translationsRepository.loadTranslation(
              movie = item.requireMovie(),
              language = language,
              onlyLocal = onlyLocal,
            )
          }

          else -> {
            throw IllegalStateException()
          }
        }
      }
  }
