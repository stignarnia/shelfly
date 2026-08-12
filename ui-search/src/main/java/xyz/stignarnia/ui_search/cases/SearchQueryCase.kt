package xyz.stignarnia.ui_search.cases

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.SearchResult
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_search.recycler.SearchListItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@ViewModelScoped
class SearchQueryCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val remoteSource: RemoteDataSource,
  private val mappers: Mappers,
  private val settingsRepository: SettingsRepository,
  private val showsRepository: ShowsRepository,
  private val moviesRepository: MoviesRepository,
  private val translationsRepository: TranslationsRepository,
  private val showsImagesProvider: ShowImagesProvider,
  private val moviesImagesProvider: MovieImagesProvider,
) {

  suspend fun searchByQuery(query: String): List<SearchListItem> =
    withContext(dispatchers.IO) {
      val withMovies = settingsRepository.isMoviesEnabled
      val myShowsIds = showsRepository.myShows.loadAllIds()
      val watchlistShowsIds = showsRepository.watchlistShows.loadAllIds()
      val myMoviesIds = moviesRepository.myMovies.loadAllIds()
      val watchlistMoviesIds = moviesRepository.watchlistMovies.loadAllIds()
      val spoilers = settingsRepository.spoilers.getAll()

      remoteSource.tmdb
        .fetchSearchResults(query)
        .mapIndexed { index, item ->
          val order = index + 1
          async {
            val result = SearchResult(
              order = order,
              show = item.show?.let { s -> mappers.show.fromNetwork(s) } ?: Show.EMPTY,
              movie = item.movie?.let { m -> mappers.movie.fromNetwork(m) } ?: Movie.EMPTY,
            )

            val isFollowed =
              if (result.isShow) {
                result.tmdbId in myShowsIds
              } else {
                result.tmdbId in myMoviesIds
              }

            val isWatchlist =
              if (result.isShow) {
                result.tmdbId in watchlistShowsIds
              } else {
                result.tmdbId in watchlistMoviesIds
              }

            val image = loadImage(result)
            val translation = loadTranslation(result)

            SearchListItem(
              id = UUID.randomUUID(),
              show = result.show,
              movie = result.movie,
              image = image,
              order = order,
              isFollowed = isFollowed,
              isWatchlist = isWatchlist,
              translation = translation,
              spoilers = spoilers,
            )
          }
        }.awaitAll()
    }

  private suspend fun loadImage(result: SearchResult) =
    when {
      result.isShow -> showsImagesProvider.findCachedImage(result.show, ImageType.POSTER)
      else -> moviesImagesProvider.findCachedImage(result.movie, ImageType.POSTER)
    }

  private suspend fun loadTranslation(result: SearchResult): Translation? {
    val language = translationsRepository.getLanguage()
    if (language == Config.DEFAULT_LANGUAGE) return Translation.EMPTY
    return when {
      result.isShow -> translationsRepository.loadTranslation(result.show, language, onlyLocal = true)
      else -> translationsRepository.loadTranslation(result.movie, language, onlyLocal = true)
    }
  }
}
