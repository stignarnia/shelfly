package xyz.stignarnia.ui_show.sections.nextepisode.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsNextEpisodeCase @Inject constructor(
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
