package xyz.stignarnia.repository.mappers

import xyz.stignarnia.dataLocal.database.model.EpisodeTranslation
import xyz.stignarnia.dataLocal.database.model.MovieTranslation
import xyz.stignarnia.dataLocal.database.model.ShowTranslation
import xyz.stignarnia.uiModel.SeasonTranslation
import xyz.stignarnia.uiModel.Translation
import javax.inject.Inject
import xyz.stignarnia.dataRemote.catalog.model.SeasonTranslation as SeasonTranslationNetwork
import xyz.stignarnia.dataRemote.catalog.model.Translation as TranslationNetwork

class TranslationMapper
  @Inject
  constructor(
    private val idsMapper: IdsMapper,
  ) {
    fun fromNetwork(value: TranslationNetwork?) =
      Translation(
        title = value?.title ?: "",
        overview = value?.overview ?: "",
        language = value?.language ?: "",
      )

    fun fromNetwork(value: SeasonTranslationNetwork?) =
      SeasonTranslation(
        ids = idsMapper.fromNetwork(value?.ids),
        seasonNumber = value?.season ?: -1,
        episodeNumber = value?.number ?: -1,
        title = value?.translations?.firstOrNull()?.title ?: "",
        overview = value?.translations?.firstOrNull()?.overview ?: "",
        language = value?.translations?.firstOrNull()?.language ?: "",
      )

    fun fromDatabase(value: ShowTranslation?) =
      Translation(
        title = value?.title ?: "",
        overview = value?.overview ?: "",
        language = value?.language ?: "",
      )

    fun fromDatabase(value: MovieTranslation?) =
      Translation(
        title = value?.title ?: "",
        overview = value?.overview ?: "",
        language = value?.language ?: "",
      )

    fun fromDatabase(value: EpisodeTranslation?) =
      Translation(
        title = value?.title ?: "",
        overview = value?.overview ?: "",
        language = value?.language ?: "",
      )
  }
