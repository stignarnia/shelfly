package xyz.stignarnia.shelfly.ui.main.cases.deeplink

import xyz.stignarnia.data_local.sources.MoviesLocalDataSource
import xyz.stignarnia.data_local.sources.ShowsLocalDataSource
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.repository.movies.MovieDetailsRepository
import xyz.stignarnia.repository.shows.ShowDetailsRepository
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.ui_model.IdImdb
import javax.inject.Inject

class ImdbDeepLinkCase @Inject constructor(
  private val tmdbRemoteSource: TmdbRemoteDataSource,
  private val showsLocalSource: ShowsLocalDataSource,
  private val moviesLocalSource: MoviesLocalDataSource,
  private val showDetailsRepository: ShowDetailsRepository,
  private val movieDetailsRepository: MovieDetailsRepository,
  private val mappers: Mappers,
) {

  suspend fun findById(imdbId: IdImdb): DeepLinkBundle {
    val show = showDetailsRepository.find(imdbId)
    if (show != null) {
      return DeepLinkBundle(show = show)
    }

    val movie = movieDetailsRepository.find(imdbId)
    if (movie != null) {
      return DeepLinkBundle(movie = movie)
    }

    tmdbRemoteSource.fetchShowByImdbId(imdbId.id)?.let {
      val uiShow = mappers.show.fromNetwork(it)
      showsLocalSource.upsert(listOf(mappers.show.toDatabase(uiShow)))
      return DeepLinkBundle(show = uiShow)
    }

    tmdbRemoteSource.fetchMovieByImdbId(imdbId.id)?.let {
      val uiMovie = mappers.movie.fromNetwork(it)
      moviesLocalSource.upsert(listOf(mappers.movie.toDatabase(uiMovie)))
      return DeepLinkBundle(movie = uiMovie)
    }

    return DeepLinkBundle.EMPTY
  }
}
