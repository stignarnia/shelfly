package xyz.stignarnia.uiSearch.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.MovieSearch
import xyz.stignarnia.dataLocal.database.model.ShowSearch
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MoviesRepository
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SearchResult
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import java.util.UUID
import javax.inject.Inject

@ViewModelScoped
class SearchSuggestionsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
    private val showsRepository: ShowsRepository,
    private val moviesRepository: MoviesRepository,
    private val translationsRepository: TranslationsRepository,
    private val settingsRepository: SettingsRepository,
    private val showsImagesProvider: ShowImagesProvider,
    private val moviesImagesProvider: MovieImagesProvider,
  ) {
    private var showsCache: List<ShowSearch>? = null
    private var moviesCache: List<MovieSearch>? = null
    private var showTranslationsCache: Map<Long, Translation>? = null
    private var movieTranslationsCache: Map<Long, Translation>? = null

    suspend fun loadSuggestions(query: String) =
      withContext(dispatchers.IO) {
        preloadCache()
        val spoilers = settingsRepository.spoilers.getAll()

        val showsDef = async { loadShows(query.trim(), 5) }
        val moviesDef = async { loadMovies(query.trim(), 5) }

        val suggestions =
          (showsDef.await() + moviesDef.await()).map {
            when (it) {
              is Show -> SearchResult(0, it, Movie.EMPTY)
              is Movie -> SearchResult(0, Show.EMPTY, it)
              else -> throw IllegalStateException()
            }
          }

        suggestions
          .map {
            async {
              val isFollowed =
                if (it.isShow) {
                  showsRepository.myShows.exists(it.show.ids.tmdb)
                } else {
                  moviesRepository.myMovies.exists(it.movie.ids.tmdb)
                }

              val isWatchlist =
                if (it.isShow) {
                  showsRepository.watchlistShows.exists(it.show.ids.tmdb)
                } else {
                  moviesRepository.watchlistMovies.exists(it.movie.ids.tmdb)
                }

              val image =
                if (it.isShow) {
                  showsImagesProvider.findCachedImage(it.show, ImageType.POSTER)
                } else {
                  moviesImagesProvider.findCachedImage(it.movie, ImageType.POSTER)
                }

              SearchListItem(
                id = UUID.randomUUID(),
                show = it.show,
                movie = it.movie,
                image = image,
                order = it.order,
                isFollowed = isFollowed,
                isWatchlist = isWatchlist,
                translation = loadTranslation(it),
                spoilers = spoilers,
              )
            }
          }.awaitAll()
          .sortedByDescending { it.votes }
      }

    suspend fun preloadCache() =
      withContext(dispatchers.IO) {
        val language = translationsRepository.getLanguage()
        val moviesEnabled = settingsRepository.isMoviesEnabled

        if (showsCache == null) {
          showsCache = localSource.shows.getAllForSearch()
        }
        if (moviesEnabled && moviesCache == null) {
          moviesCache = localSource.movies.getAllForSearch()
        }

        if (translationsRepository.getLanguage() != Config.DEFAULT_LANGUAGE) {
          if (showTranslationsCache == null) {
            showTranslationsCache = translationsRepository.loadAllShowsLocal(language)
          }
          if (moviesEnabled && movieTranslationsCache == null) {
            movieTranslationsCache = translationsRepository.loadAllMoviesLocal(language)
          }
        }
      }

    private suspend fun loadShows(
      query: String,
      limit: Int,
    ): List<Show> {
      if (query.trim().isBlank()) {
        return emptyList()
      }

      val cachedIds =
        showsCache
          ?.filter {
            it.title.contains(query, true) ||
              showTranslationsCache?.get(it.idTmdb)?.title?.contains(query, true) == true
          }?.take(limit)
          ?.map { it.idTmdb }

      return localSource.shows
        .getAll(cachedIds ?: emptyList())
        .map { mappers.show.fromDatabase(it) }
    }

    private suspend fun loadMovies(
      query: String,
      limit: Int,
    ): List<Movie> {
      if (query.trim().isBlank()) {
        return emptyList()
      }

      val cachedIds =
        moviesCache
          ?.filter {
            it.title.contains(query, true) ||
              movieTranslationsCache?.get(it.idTmdb)?.title?.contains(query, true) == true
          }?.take(limit)
          ?.map { it.idTmdb }

      return localSource.movies
        .getAll(cachedIds ?: emptyList())
        .map { mappers.movie.fromDatabase(it) }
    }

    private suspend fun loadTranslation(result: SearchResult): Translation? {
      val language = translationsRepository.getLanguage()
      if (language == Config.DEFAULT_LANGUAGE) return Translation.EMPTY
      return when {
        result.isShow -> translationsRepository.loadTranslation(result.show, language, onlyLocal = true)
        else -> translationsRepository.loadTranslation(result.movie, language, onlyLocal = true)
      }
    }

    fun clearCache() {
      showsCache = null
      moviesCache = null
      showTranslationsCache = null
      movieTranslationsCache = null
    }
  }
