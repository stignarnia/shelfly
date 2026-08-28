package xyz.stignarnia.uiMyMovies.mymovies.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.removeDiacritics
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiMyMovies.mymovies.helpers.MyMoviesSorter
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem
import javax.inject.Inject

@ViewModelScoped
class MyMoviesLoadCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val sorter: MyMoviesSorter,
    private val imagesProvider: MovieImagesProvider,
    private val moviesRepository: MoviesRepository,
    private val dateFormatProvider: DateFormatProvider,
    private val translationsRepository: TranslationsRepository,
    private val settingsRepository: SettingsRepository,
  ) {
    suspend fun loadSettings() =
      withContext(dispatchers.IO) {
        settingsRepository.load()
      }

    suspend fun loadAll() =
      withContext(dispatchers.IO) {
        moviesRepository.myMovies.loadAll()
      }

    fun filterSectionMovies(
      allMovies: List<MyMoviesItem>,
      sortOrder: Pair<SortOrder, SortType>,
      genres: List<String>,
      searchQuery: String? = null,
    ) = allMovies
      .filterByQuery(searchQuery)
      .filterByGenre(genres)
      .sortedWith(sorter.sort(sortOrder.first, sortOrder.second))

    private fun List<MyMoviesItem>.filterByQuery(query: String?) =
      when {
        query.isNullOrBlank() -> {
          this
        }

        else -> {
          this.filter {
            it.movie.title
              .removeDiacritics()
              .contains(query, true) ||
              it.translation
                ?.title
                ?.removeDiacritics()
                ?.contains(query, true) == true
          }
        }
      }

    private fun List<MyMoviesItem>.filterByGenre(genres: List<String>) =
      filter { genres.isEmpty() || it.movie.genres.any { genre -> genre.lowercase() in genres } }

    suspend fun loadRecentMovies(): List<Movie> =
      withContext(dispatchers.IO) {
        val amount = loadSettings().myRecentsAmount
        moviesRepository.myMovies.loadAllRecent(amount)
      }

    suspend fun loadTranslation(
      movie: Movie,
      onlyLocal: Boolean,
    ): Translation? =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        if (language == Config.DEFAULT_LANGUAGE) {
          return@withContext Translation.EMPTY
        }
        translationsRepository.loadTranslation(movie, language, onlyLocal)
      }

    fun loadDateFormat() = dateFormatProvider.loadShortDayFormat()

    suspend fun findCachedImage(
      movie: Movie,
      type: ImageType,
    ) = imagesProvider.findCachedImage(movie, type)

    suspend fun loadMissingImage(
      movie: Movie,
      type: ImageType,
      force: Boolean,
    ) = imagesProvider.loadRemoteImage(movie, type, force)
  }
