package xyz.stignarnia.uiShow.sections.nextepisode.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsNextEpisodeCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val remoteSource: RemoteDataSource,
    private val mappers: Mappers,
  ) {
    suspend fun loadNextEpisode(tmdbId: IdTmdb): Episode? =
      withContext(dispatchers.IO) {
        val episode = remoteSource.tmdb.fetchNextEpisode(tmdbId.id) ?: return@withContext null
        return@withContext mappers.episode.fromNetwork(episode)
      }
  }
