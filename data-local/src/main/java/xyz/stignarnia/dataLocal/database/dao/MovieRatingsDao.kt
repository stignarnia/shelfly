package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import xyz.stignarnia.dataLocal.database.model.MovieRatings
import xyz.stignarnia.dataLocal.sources.MovieRatingsLocalDataSource

@Dao
interface MovieRatingsDao :
  BaseDao<MovieRatings>,
  MovieRatingsLocalDataSource {
  @Transaction
  override suspend fun upsert(entity: MovieRatings) {
    val local = getById(entity.idTmdb)
    if (local != null) {
      update(
        listOf(
          local.copy(
            tmdb = entity.tmdb,
            imdb = entity.imdb,
            metascore = entity.metascore,
            rottenTomatoes = entity.rottenTomatoes,
            rottenTomatoesUrl = entity.rottenTomatoesUrl,
            updatedAt = entity.updatedAt,
          ),
        ),
      )
      return
    }
    insert(listOf(entity))
  }

  @Query("SELECT * FROM movies_ratings WHERE id_tmdb == :tmdbId")
  override suspend fun getById(tmdbId: Long): MovieRatings?
}
