package xyz.stignarnia.ui_progress.progress.recycler

import androidx.annotation.StringRes
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_progress.helpers.TranslationsBundle
import java.time.format.DateTimeFormatter
import xyz.stignarnia.ui_model.Episode as EpisodeModel

sealed class ProgressListItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean = false,
) : ListItem {

  data class Episode(
    override val show: Show,
    override val image: Image,
    override val isLoading: Boolean = false,
    val episode: EpisodeModel?,
    val season: Season?,
    val totalCount: Int,
    val watchedCount: Int,
    val isWatched: Boolean,
    val isUpcoming: Boolean,
    val isPinned: Boolean,
    val isOnHold: Boolean,
    val translations: TranslationsBundle? = null,
    val dateFormat: DateTimeFormatter? = null,
    val sortOrder: SortOrder? = null,
    val userRating: Int? = null,
    val spoilers: SpoilersSettings? = null,
  ) : ProgressListItem(show, image, isLoading) {

    fun isNew() =
      episode?.firstAired?.isBefore(nowUtc()) ?: false &&
        nowUtcMillis() - (episode.firstAired?.toMillis() ?: 0) < Config.NEW_BADGE_DURATION

    fun requireEpisode() = episode!!

    fun requireSeason() = season!!
  }

  data class Header(
    override val show: Show,
    override val image: Image,
    override val isLoading: Boolean = false,
    val type: Type,
    @get:StringRes val textResId: Int,
    val isCollapsed: Boolean,
  ) : ProgressListItem(show, image, isLoading) {

    companion object {
      fun create(
        type: Type,
        @StringRes textResId: Int,
        isCollapsed: Boolean,
      ) = Header(
        type = type,
        show = Show.EMPTY,
        image = Image.createUnavailable(ImageType.POSTER),
        textResId = textResId,
        isCollapsed = isCollapsed,
      )
    }

    override fun isSameAs(other: ListItem) = textResId == (other as? Header)?.textResId

    enum class Type {
      UPCOMING,
      ON_HOLD,
    }
  }

  data class Filters(
    val sortOrder: SortOrder,
    val sortType: SortType,
    val isUpcoming: Boolean,
    val isUpcomingEnabled: Boolean,
    val isOnHold: Boolean,
    val newAtTop: Boolean,
  ) : ProgressListItem(
      show = Show.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {

    fun hasActiveFilters() = isUpcoming || isOnHold
  }
}
