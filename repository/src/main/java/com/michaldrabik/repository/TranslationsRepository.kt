package com.michaldrabik.repository

import android.content.SharedPreferences
import com.michaldrabik.common.Config.DEFAULT_LANGUAGE
import com.michaldrabik.common.ConfigVariant
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.EpisodeTranslation
import com.michaldrabik.data_local.database.model.MovieTranslation
import com.michaldrabik.data_local.database.model.ShowTranslation
import com.michaldrabik.data_local.database.model.TranslationsMoviesSyncLog
import com.michaldrabik.data_local.database.model.TranslationsSyncLog
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.repository.settings.SettingsRepository.Key.LANGUAGE
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.SeasonTranslation
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.Translation
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import com.michaldrabik.data_remote.catalog.model.Translation as TranslationRemote

@Singleton
class TranslationsRepository @Inject constructor(
  @Named("miscPreferences") private var miscPreferences: SharedPreferences,
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  fun getLanguage() = miscPreferences.getString(LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

  suspend fun loadAllShowsLocal(language: String = DEFAULT_LANGUAGE): Map<Long, Translation> {
    val local = localSource.showTranslations.getAll(language)
    return local.associate {
      Pair(it.idTmdb, mappers.translation.fromDatabase(it))
    }
  }

  suspend fun loadAllMoviesLocal(language: String = DEFAULT_LANGUAGE): Map<Long, Translation> {
    val local = localSource.movieTranslations.getAll(language)
    return local.associate {
      Pair(it.idTmdb, mappers.translation.fromDatabase(it))
    }
  }

  suspend fun loadTranslation(
    show: Show,
    language: String = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val local = localSource.showTranslations.getById(show.tmdbId, language)
    local?.let {
      return mappers.translation.fromDatabase(it)
    }
    if (onlyLocal) return null

    val timestamp = localSource.translationsShowsSyncLog.getById(show.tmdbId)?.syncedAt ?: 0
    if (nowUtcMillis() - timestamp < ConfigVariant.TRANSLATION_SYNC_SHOW_MOVIE_COOLDOWN) {
      return Translation.EMPTY
    }

    val remoteTranslation = try {
      remoteSource.tmdb
        .fetchShowTranslation(show.tmdbId, language)
    } catch (error: Throwable) {
      null
    }

    val translation = mappers.translation.fromNetwork(remoteTranslation)
    val translationDb = ShowTranslation.fromTmdbId(
      show.tmdbId,
      translation.title,
      language,
      translation.overview,
      nowUtcMillis(),
    )

    if (translationDb.overview.isNotBlank() || translationDb.title.isNotBlank()) {
      localSource.showTranslations.insertSingle(translationDb)
    }
    localSource.translationsShowsSyncLog.upsert(TranslationsSyncLog(show.tmdbId, nowUtcMillis()))

    return translation
  }

  suspend fun loadTranslation(
    movie: Movie,
    language: String = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val local = localSource.movieTranslations.getById(movie.tmdbId, language)
    local?.let {
      return mappers.translation.fromDatabase(it)
    }
    if (onlyLocal) return null

    val timestamp = localSource.translationsMoviesSyncLog.getById(movie.tmdbId)?.syncedAt ?: 0
    if (nowUtcMillis() - timestamp < ConfigVariant.TRANSLATION_SYNC_SHOW_MOVIE_COOLDOWN) {
      return Translation.EMPTY
    }

    val remoteTranslation = try {
      remoteSource.tmdb
        .fetchMovieTranslation(movie.tmdbId, language)
    } catch (error: Throwable) {
      null
    }

    val translation = mappers.translation.fromNetwork(remoteTranslation)
    val translationDb = MovieTranslation.fromTmdbId(
      movie.tmdbId,
      translation.title,
      language,
      translation.overview,
      nowUtcMillis(),
    )

    if (translationDb.overview.isNotBlank() || translationDb.title.isNotBlank()) {
      localSource.movieTranslations.insertSingle(translationDb)
    }
    localSource.translationsMoviesSyncLog.upsert(TranslationsMoviesSyncLog(movie.tmdbId, nowUtcMillis()))

    return translation
  }

  suspend fun loadTranslation(
    episode: Episode,
    showId: IdTmdb,
    language: String = DEFAULT_LANGUAGE,
    onlyLocal: Boolean = false,
  ): Translation? {
    val nowMillis = nowUtcMillis()
    val local = localSource.episodesTranslations.getById(episode.ids.tmdb.id, showId.id, language)
    local?.let {
      val isCacheValid = nowMillis - it.updatedAt < ConfigVariant.TRANSLATION_SYNC_EPISODE_COOLDOWN
      if (it.title.isNotBlank() && it.overview.isNotBlank()) {
        return mappers.translation.fromDatabase(it)
      }
      if ((it.title.isNotBlank() || it.overview.isNotBlank()) && (isCacheValid || onlyLocal)) {
        return mappers.translation.fromDatabase(it)
      }
    }

    if (onlyLocal) return null

    val remoteTranslations = remoteSource.tmdb
      .fetchSeasonTranslations(showId.id, episode.season, language)
      .map { mappers.translation.fromNetwork(it) }

    remoteTranslations
      .forEach { item ->
        val dbItem = EpisodeTranslation.fromTmdbId(
          traktEpisodeId = item.ids.tmdb.id,
          traktShowId = showId.id,
          title = item.title,
          overview = item.overview,
          language = language,
          createdAt = nowMillis,
        )
        localSource.episodesTranslations.insertSingle(dbItem)
      }

    remoteTranslations
      .find { it.ids.tmdb == episode.ids.tmdb }
      ?.let {
        return Translation(it.title, it.overview, it.language)
      }

    return null
  }

  suspend fun loadTranslations(
    season: Season,
    showId: IdTmdb,
    language: String = DEFAULT_LANGUAGE,
  ): List<SeasonTranslation> {
    val episodes = season.episodes.toList()
    val episodesIds = season.episodes.map { it.ids.tmdb.id }

    val local = localSource.episodesTranslations.getByIds(episodesIds, showId.id, language)
    val hasAllTranslated = local.isNotEmpty() && local.all { it.title.isNotBlank() && it.overview.isNotBlank() }
    val isCacheValid =
      local.isNotEmpty() && nowUtcMillis() - local.first().updatedAt < ConfigVariant.TRANSLATION_SYNC_EPISODE_COOLDOWN

    if (hasAllTranslated || (!hasAllTranslated && isCacheValid)) {
      return episodes.map { episode ->
        val translation = local.find { it.idTmdb == episode.ids.tmdb.id }
        SeasonTranslation(
          ids = episode.ids.copy(),
          title = translation?.title ?: "",
          overview = translation?.overview ?: "",
          seasonNumber = season.number,
          episodeNumber = episode.number,
          language = language,
          isLocal = true,
        )
      }
    }

    val remoteTranslation = remoteSource.tmdb
      .fetchSeasonTranslations(showId.id, season.number, language)
      .map { mappers.translation.fromNetwork(it) }

    remoteTranslation
      .forEach { item ->
        val dbItem = EpisodeTranslation.fromTmdbId(
          item.ids.tmdb.id,
          showId.id,
          item.title,
          language,
          item.overview,
          nowUtcMillis(),
        )
        localSource.episodesTranslations.insertSingle(dbItem)
      }

    return episodes.map { episode ->
      val translation = remoteTranslation.find { it.ids.tmdb.id == episode.ids.tmdb.id }
      SeasonTranslation(
        ids = episode.ids.copy(),
        title = translation?.title ?: "",
        overview = translation?.overview ?: "",
        seasonNumber = season.number,
        episodeNumber = episode.number,
        language = language,
        isLocal = true,
      )
    }
  }

  private fun chineseLanguagePredicate(translation: TranslationRemote) =
    if (translation.language?.lowercase() != "zh") {
      true
    } else {
      translation.country?.equals("cn", ignoreCase = true) == true
    }

  private fun frenchLanguagePredicate(translation: TranslationRemote) =
    if (translation.language?.lowercase() != "fr") {
      true
    } else {
      translation.country?.equals("fr", ignoreCase = true) == true
    }
}
