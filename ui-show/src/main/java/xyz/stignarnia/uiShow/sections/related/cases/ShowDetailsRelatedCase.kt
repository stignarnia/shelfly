package xyz.stignarnia.uiShow.sections.related.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsRelatedCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val showsRepository: ShowsRepository,
  ) {
    suspend fun loadRelatedShows(show: Show): List<Show> =
      withContext(dispatchers.IO) {
        val archivedShowsIds = showsRepository.hiddenShows.loadAllIds()
        showsRepository.relatedShows
          .loadAll(show, archivedShowsIds.size)
          .filter { it.tmdbId !in archivedShowsIds }
          .sortedWith(compareBy({ it.votes }, { it.rating }))
          .reversed()
      }
  }
