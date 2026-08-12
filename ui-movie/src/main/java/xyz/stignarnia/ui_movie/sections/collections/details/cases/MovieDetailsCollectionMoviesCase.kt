package xyz.stignarnia.ui_movie.sections.collections.details.cases

import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MovieCollectionsRepository
import xyz.stignarnia.repository.movies.MyMoviesRepository
import xyz.stignarnia.repository.movies.WatchlistMoviesRepository
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.ImageType.POSTER
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_movie.sections.collections.details.recycler.MovieDetailsCollectionItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsCollectionMoviesCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val collectionsRepository: MovieCollectionsRepository,
  private val myMoviesRepository: MyMoviesRepository,
  private val watchlistMoviesRepository: WatchlistMoviesRepository,
  private val translationsRepository: TranslationsRepository,
  private val settingsSpoilersRepository: SettingsSpoilersRepository,
  private val imagesProvider: MovieImagesProvider,
) {

  suspend fun loadCollectionMovies(
    collectionId: IdTmdb,
    language: String,
  ): List<MovieDetailsCollectionItem.MovieItem> =
    withContext(dispatchers.IO) {
      val movies = collectionsRepository.loadCollectionItems(collectionId)
      movies
        .mapIndexed { index, movie ->
          async {
            MovieDetailsCollectionItem.MovieItem(
              rank = index + 1,
              movie = movie,
              image = imagesProvider.findCachedImage(movie, POSTER),
              isMyMovie = myMoviesRepository.exists(movie.ids.tmdb),
              isWatchlist = watchlistMoviesRepository.exists(movie.ids.tmdb),
              translation = loadTranslation(movie, language),
              spoilers = settingsSpoilersRepository.getAll(),
              isLoading = false,
            )
          }
        }.awaitAll()
    }

  private suspend fun loadTranslation(
    movie: Movie,
    language: String,
  ): Translation? {
    if (language == DEFAULT_LANGUAGE) return null
    return translationsRepository.loadTranslation(
      movie = movie,
      language = language,
      onlyLocal = true,
    )
  }
}
