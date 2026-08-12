package xyz.stignarnia.ui_my_shows.myshows.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Season
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.utilities.extensions.removeDiacritics
import xyz.stignarnia.ui_model.MyShowsSection.FINISHED
import xyz.stignarnia.ui_model.MyShowsSection.UPCOMING
import xyz.stignarnia.ui_model.MyShowsSection.WATCHING
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.ShowStatus.RETURNING
import xyz.stignarnia.ui_my_shows.myshows.helpers.MyShowsItemSorter
import xyz.stignarnia.ui_my_shows.myshows.recycler.MyShowsItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MyShowsLoadShowsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val sorter: MyShowsItemSorter,
  private val showsRepository: ShowsRepository,
  private val settingsRepository: SettingsRepository,
  private val localSource: LocalDataSource,
) {

  suspend fun loadAllShows() =
    withContext(dispatchers.IO) {
      showsRepository.myShows.loadAll()
    }

  suspend fun loadRecentShows(): List<Show> =
    withContext(dispatchers.IO) {
      val amount = settingsRepository.load().myRecentsAmount
      showsRepository.myShows.loadAllRecent(amount)
    }

  suspend fun loadSeasonsForShows(
    tmdbIds: List<Long>,
    buffer: MutableList<Season> = mutableListOf(),
  ): List<Season> =
    withContext(dispatchers.IO) {
      val batch = tmdbIds.take(500)
      if (batch.isEmpty()) {
        return@withContext buffer
      }

      val seasons = localSource.seasons
        .getAllByShowsIds(batch)
        .filter { it.seasonNumber != 0 }
      buffer.addAll(seasons)

      loadSeasonsForShows(tmdbIds.filter { it !in batch }, buffer)
    }

  fun filterSectionShows(
    allShows: List<MyShowsItem>,
    allSeasons: List<Season>,
    searchQuery: String? = null,
    networks: List<String>,
    genres: List<String>,
  ): List<MyShowsItem> {
    val shows = allShows
      .filter { showItem ->
        val seasons = allSeasons.filter { it.idShowTmdb == showItem.show.tmdbId }
        val airedSeasons = seasons.filter { it.seasonFirstAired?.isBefore(nowUtc()) == true }

        when (val type = settingsRepository.filters.myShowsType) {
          WATCHING -> {
            airedSeasons.any { !it.isWatched }
          }
          FINISHED -> {
            type.allowedStatuses.contains(showItem.show.status) && seasons.all { it.isWatched }
          }
          UPCOMING -> {
            type.allowedStatuses.contains(showItem.show.status) ||
              (showItem.show.status == RETURNING && airedSeasons.all { it.isWatched })
          }
          else -> {
            true
          }
        }
      }

    return shows
      .filterByQuery(searchQuery)
      .filterByNetwork(networks)
      .filterByGenre(genres)
      .sortedWith(
        sorter.sort(
          sortOrder = settingsRepository.sorting.myShowsAllSortOrder,
          sortType = settingsRepository.sorting.myShowsAllSortType,
        ),
      )
  }

  private fun List<MyShowsItem>.filterByQuery(query: String?) =
    when {
      query.isNullOrBlank() -> this
      else -> this.filter {
        it.show.title
          .removeDiacritics()
          .contains(query, true) ||
          it.translation
            ?.title
            ?.removeDiacritics()
            ?.contains(query, true) == true
      }
    }

  private fun List<MyShowsItem>.filterByNetwork(networks: List<String>) =
    filter { networks.isEmpty() || it.show.network in networks }

  private fun List<MyShowsItem>.filterByGenre(genres: List<String>) =
    filter { genres.isEmpty() || it.show.genres.any { genre -> genre.lowercase() in genres } }
}
