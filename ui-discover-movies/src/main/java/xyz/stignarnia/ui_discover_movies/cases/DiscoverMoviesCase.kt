package xyz.stignarnia.ui_discover_movies.cases

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtcDay
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.ui_discover_movies.helpers.itemtype.ImageTypeProvider
import xyz.stignarnia.ui_discover_movies.recycler.DiscoverMovieListItem
import xyz.stignarnia.ui_model.DiscoverFeed
import xyz.stignarnia.ui_model.DiscoverFilters
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.ImageType.POSTER
import xyz.stignarnia.ui_model.Movie
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
internal class DiscoverMoviesCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val moviesRepository: MoviesRepository,
  private val imagesProvider: MovieImagesProvider,
  private val imageTypeProvider: ImageTypeProvider,
  private val translationsRepository: TranslationsRepository,
) {

  suspend fun isCacheValid() =
    withContext(dispatchers.IO) {
      moviesRepository.discoverMovies.isCacheValid()
    }

  suspend fun loadCachedMovies(filters: DiscoverFilters) =
    withContext(dispatchers.IO) {
      val myIds = async { moviesRepository.myMovies.loadAllIds() }
      val watchlistIds = async { moviesRepository.watchlistMovies.loadAllIds() }
      val hiddenIds = async { moviesRepository.hiddenMovies.loadAllIds() }
      val cachedMovies = async { moviesRepository.discoverMovies.loadAllCached() }
      val language = translationsRepository.getLanguage()

      prepareItems(
        cachedMovies.await(),
        myIds.await(),
        watchlistIds.await(),
        hiddenIds.await(),
        filters,
        language,
      )
    }

  suspend fun loadRemoteMovies(filters: DiscoverFilters) =
    withContext(dispatchers.IO) {
      val showCollection = !filters.hideCollection
      val genres = filters.genres.toList()

      val myAsync = async { moviesRepository.myMovies.loadAllIds() }
      val watchlistSync = async { moviesRepository.watchlistMovies.loadAllIds() }
      val hiddenAsync = async { moviesRepository.hiddenMovies.loadAllIds() }
      val (myIds, watchlistIds, hiddenIds) = awaitAll(myAsync, watchlistSync, hiddenAsync)
      val collectionSize = myIds.size + watchlistIds.size + hiddenIds.size

      val remoteMovies = moviesRepository.discoverMovies.loadAllRemote(
        filters.feedOrder,
        showCollection,
        collectionSize,
        genres,
      )
      val language = translationsRepository.getLanguage()

      moviesRepository.discoverMovies.cacheDiscoverMovies(remoteMovies)
      prepareItems(remoteMovies, myIds, watchlistIds, hiddenIds, filters, language)
    }

  private suspend fun prepareItems(
    movies: List<Movie>,
    myMoviesIds: List<Long>,
    watchlistMoviesIds: List<Long>,
    hiddenMoviesIds: List<Long>,
    filters: DiscoverFilters,
    language: String,
  ) = coroutineScope {
    val collectionIds = myMoviesIds + watchlistMoviesIds
    movies
      .filter { !hiddenMoviesIds.contains(it.tmdbId) }
      .filter {
        if (!filters.hideCollection) {
          true
        } else {
          !collectionIds.contains(it.tmdbId)
        }
      }.sortedBy(filters.feedOrder)
      .mapIndexed { index, movie ->
        async {
          val itemType = imageTypeProvider.getImageType(index)
          val image = imagesProvider.findCachedImage(movie, itemType)
          val translation = loadTranslation(language, itemType, movie)
          DiscoverMovieListItem(
            movie,
            image,
            isCollected = movie.ids.tmdb.id in myMoviesIds,
            isWatchlist = movie.ids.tmdb.id in watchlistMoviesIds,
            translation = translation,
          )
        }
      }.awaitAll()
      .toList()
  }

  private suspend fun loadTranslation(
    language: String,
    itemType: ImageType,
    movie: Movie,
  ) = if (language == Config.DEFAULT_LANGUAGE || itemType == POSTER) {
    null
  } else {
    translationsRepository.loadTranslation(movie, language, true)
  }

  private fun List<Movie>.sortedBy(order: DiscoverFeed): List<Movie> {
    val nowUtcDay = nowUtcDay()
    return when (order) {
      DiscoverFeed.RECENT -> {
        this
          .filter {
            it.released != null && (it.released!!.isBefore(nowUtcDay) || it.released!!.isEqual(nowUtcDay))
          }.sortedWith(compareByDescending { it.released })
      }
      else -> {
        this
      }
    }
  }
}
