package xyz.stignarnia.ui_people.details.recycler

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Person
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_people.details.filters.PersonDetailsFilters
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
