package xyz.stignarnia.shelfly.ui.main.cases.deeplink

import xyz.stignarnia.dataLocal.sources.MoviesLocalDataSource
import xyz.stignarnia.dataLocal.sources.ShowsLocalDataSource
import xyz.stignarnia.dataRemote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MovieDetailsRepository
import xyz.stignarnia.repository.shows.ShowDetailsRepository
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkResolver.Companion.TMDB_TYPE_MOVIE
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkResolver.Companion.TMDB_TYPE_TV
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

class TmdbDeepLinkCase
  @Inject
  constructor(
    private val tmdbRemoteSource: TmdbRemoteDataSource,
    private val showsLocalSource: ShowsLocalDataSource,
    private val moviesLocalSource: MoviesLocalDataSource,
    private val showDetailsRepository: ShowDetailsRepository,
    private val movieDetailsRepository: MovieDetailsRepository,
    private val mappers: Mappers,
  ) {
    suspend fun findById(
      tmdbId: IdTmdb,
      type: String,
    ): DeepLinkBundle {
      val localShow = showDetailsRepository.find(tmdbId)
      if (localShow != null && type == TMDB_TYPE_TV) {
        return DeepLinkBundle(show = localShow)
      }

      val localMovie = movieDetailsRepository.find(tmdbId)
      if (localMovie != null && type == TMDB_TYPE_MOVIE) {
        return DeepLinkBundle(movie = localMovie)
      }

      if (type == TMDB_TYPE_TV) {
        val uiShow = mappers.show.fromNetwork(tmdbRemoteSource.fetchShow(tmdbId.id))
        showsLocalSource.upsert(listOf(mappers.show.toDatabase(uiShow)))
        return DeepLinkBundle(show = uiShow)
      }

      if (type == TMDB_TYPE_MOVIE) {
        val uiMovie = mappers.movie.fromNetwork(tmdbRemoteSource.fetchMovie(tmdbId.id))
        moviesLocalSource.upsert(listOf(mappers.movie.toDatabase(uiMovie)))
        return DeepLinkBundle(movie = uiMovie)
      }

      return DeepLinkBundle.EMPTY
    }
  }
