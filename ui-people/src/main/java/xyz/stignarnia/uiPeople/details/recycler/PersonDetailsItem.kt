package xyz.stignarnia.uiPeople.details.recycler

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiPeople.details.filters.PersonDetailsFilters
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class PersonDetailsItem {
  open fun getId(): String = UUID.randomUUID().toString()

  open fun getReleaseDate(): LocalDate? = null

  fun isCreditsItem() = this is CreditsHeader || this is CreditsMovieItem || this is CreditsShowItem

  data class MainInfo(
    val person: Person,
    val dateFormat: DateTimeFormatter?,
    val isLoading: Boolean = false,
  ) : PersonDetailsItem()

  data class MainBio(
    val biography: String?,
    val biographyTranslation: String?,
  ) : PersonDetailsItem()

  data class CreditsHeader(
    val year: Int?,
  ) : PersonDetailsItem() {
    override fun getId() = year?.toString() ?: ""
  }

  data class CreditsShowItem(
    val show: Show,
    val image: Image,
    val isMy: Boolean,
    val isWatchlist: Boolean,
    val translation: Translation?,
    val spoilers: SpoilersSettings,
    val isLoading: Boolean = false,
  ) : PersonDetailsItem() {
    override fun getId() = "${show.tmdbId}show"

    override fun getReleaseDate() =
      if (show.firstAired.isNotBlank()) {
        ZonedDateTime.parse(show.firstAired).toLocalDate()
      } else {
        null
      }
  }

  data class CreditsMovieItem(
    val movie: Movie,
    val image: Image,
    val isMy: Boolean,
    val isWatchlist: Boolean,
    val translation: Translation?,
    val spoilers: SpoilersSettings,
    val moviesEnabled: Boolean,
    val isLoading: Boolean = false,
  ) : PersonDetailsItem() {
    override fun getId() = "${movie.tmdbId}movie"

    override fun getReleaseDate() = movie.released
  }

  data class CreditsFiltersItem(
    val filters: PersonDetailsFilters,
  ) : PersonDetailsItem()

  data object CreditsLoadingItem : PersonDetailsItem()
}
