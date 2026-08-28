package xyz.stignarnia.uiMovie.sections.collections.details.recycler

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import java.util.UUID

sealed class MovieDetailsCollectionItem {
  open val id: String
    get() = UUID.randomUUID().toString()

  data class HeaderItem(
    val title: String,
    val description: String,
  ) : MovieDetailsCollectionItem()

  data class MovieItem(
    val rank: Int,
    val movie: Movie,
    val image: Image,
    val isMyMovie: Boolean,
    val isWatchlist: Boolean,
    val translation: Translation?,
    val spoilers: SpoilersSettings,
    val isLoading: Boolean,
  ) : MovieDetailsCollectionItem() {
    override val id get() = "${movie.tmdbId}"
  }

  object LoadingItem : MovieDetailsCollectionItem()
}
