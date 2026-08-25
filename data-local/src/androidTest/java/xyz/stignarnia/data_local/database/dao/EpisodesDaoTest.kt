
package xyz.stignarnia.data_local.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_local.database.dao.helpers.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodesDaoTest : BaseDaoTest() {

  @Test
  fun shouldStoreEpisodesForSeason() {
    runBlocking {
      val season = TestData.createSeason()
      val episode1 = TestData.createEpisode().copy(idTmdb = 1)
      val episode2 = TestData.createEpisode().copy(idTmdb = 2)

      database.seasonsDao().upsert(listOf(season))
      database.episodesDao().upsert(listOf(episode1, episode2))

      val result = database.episodesDao().getAllForSeason(1)
      assertThat(result).containsExactlyElementsIn(listOf(episode1, episode2))
    }
  }

  @Test
  fun shouldUpdateEpisodeIfAlreadyExists() {
    runBlocking {
      val season = TestData.createSeason()
      val episode1 = TestData.createEpisode().copy(idTmdb = 1)
      val episode2 = TestData.createEpisode().copy(idTmdb = 2)

      database.seasonsDao().upsert(listOf(season))
      database.episodesDao().upsert(listOf(episode1, episode2))

      val result = database.episodesDao().getAllForSeason(1)
      assertThat(result).containsExactlyElementsIn(listOf(episode1, episode2))

      val updated = episode2.copy(title = "Updated")
      database.episodesDao().upsert(listOf(episode1, updated))

      val result2 = database.episodesDao().getAllForSeason(1)
      assertThat(result2).containsExactlyElementsIn(listOf(episode1, updated))
    }
  }

  @Test
  fun shouldStoreEpisodesForShows() {
    runBlocking {
      val show1 = TestData.createShow().copy(idTmdb = 1)
      val show2 = TestData.createShow().copy(idTmdb = 2)

      val season1 = TestData.createSeason().copy(idShowTmdb = show1.idTmdb)
      val season2 = TestData.createSeason().copy(idShowTmdb = show2.idTmdb)

      val episode1 = TestData.createEpisode().copy(
        idTmdb = 1,
        idShowTmdb = show1.idTmdb,
        idSeason = season1.idTmdb,
      )
      val episode2 = TestData.createEpisode().copy(
        idTmdb = 2,
        idShowTmdb = show2.idTmdb,
        idSeason = season2.idTmdb,
      )

      database.showsDao().upsert(listOf(show1, show2))
      database.seasonsDao().upsert(listOf(season1, season2))
      database.episodesDao().upsert(listOf(episode1, episode2))

      val result2 = database.episodesDao().getAllByShowId(2)
      assertThat(result2).containsExactlyElementsIn(listOf(episode2))
    }
  }

  @Test
  fun shouldReturnWatchedIdsForShow() {
    runBlocking {
      val show = TestData.createShow().copy(idTmdb = 1)

      val season1 = TestData.createSeason().copy(idShowTmdb = show.idTmdb)
      val season2 = TestData.createSeason().copy(idShowTmdb = show.idTmdb)

      val episode1 = TestData.createEpisode().copy(
        idTmdb = 1,
        idShowTmdb = show.idTmdb,
        idSeason = season1.idTmdb,
        isWatched = true,
      )
      val episode2 = TestData.createEpisode().copy(
        idTmdb = 2,
        idShowTmdb = show.idTmdb,
        idSeason = season2.idTmdb,
        isWatched = false,
      )

      database.showsDao().upsert(listOf(show))
      database.seasonsDao().upsert(listOf(season1, season2))
      database.episodesDao().upsert(listOf(episode1, episode2))

      val result = database.episodesDao().getAllWatchedIdsForShows(listOf(show.idTmdb))
      assertThat(result).containsExactlyElementsIn(listOf(episode1.idTmdb))
    }
  }

  @Test
  fun shouldDeleteAllUnwatchedForShow() {
    runBlocking {
      val show = TestData.createShow().copy(idTmdb = 1)

      val season1 = TestData.createSeason().copy(idShowTmdb = show.idTmdb)
      val season2 = TestData.createSeason().copy(idShowTmdb = show.idTmdb)

      val episode1 = TestData.createEpisode().copy(
        idTmdb = 1,
        idShowTmdb = show.idTmdb,
        idSeason = season1.idTmdb,
        isWatched = true,
      )
      val episode2 = TestData.createEpisode().copy(
        idTmdb = 2,
        idShowTmdb = show.idTmdb,
        idSeason = season2.idTmdb,
        isWatched = false,
      )

      database.showsDao().upsert(listOf(show))
      database.seasonsDao().upsert(listOf(season1, season2))
      database.episodesDao().upsert(listOf(episode1, episode2))

      val result = database.episodesDao().getAllByShowId(show.idTmdb)
      assertThat(result).containsExactlyElementsIn(listOf(episode1, episode2))

      database.episodesDao().deleteAllUnwatchedForShow(show.idTmdb)

      val result2 = database.episodesDao().getAllByShowId(show.idTmdb)
      assertThat(result2).containsExactlyElementsIn(listOf(episode1))
    }
  }
}
