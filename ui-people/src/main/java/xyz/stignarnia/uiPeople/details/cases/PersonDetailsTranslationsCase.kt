package xyz.stignarnia.uiPeople.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem
import javax.inject.Inject

@ViewModelScoped
class PersonDetailsTranslationsCase
  @Inject
  constructor(
    private val translationsRepository: TranslationsRepository,
  ) {
    suspend fun loadMissingTranslation(
      item: PersonDetailsItem.CreditsShowItem,
      language: String,
    ) = try {
      val translation = translationsRepository.loadTranslation(item.show, language) ?: Translation.EMPTY
      item.copy(isLoading = false, translation = translation)
    } catch (error: Throwable) {
      Timber.w(error)
      item.copy(isLoading = false, translation = Translation.EMPTY)
    }

    suspend fun loadMissingTranslation(
      item: PersonDetailsItem.CreditsMovieItem,
      language: String,
    ) = try {
      val translation = translationsRepository.loadTranslation(item.movie, language) ?: Translation.EMPTY
      item.copy(isLoading = false, translation = translation)
    } catch (error: Throwable) {
      Timber.w(error)
      item.copy(isLoading = false, translation = Translation.EMPTY)
    }
  }
