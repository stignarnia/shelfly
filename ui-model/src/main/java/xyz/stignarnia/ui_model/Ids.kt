package xyz.stignarnia.ui_model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * TMDB is the catalog, so its id is the identity of a show or movie throughout
 * the app and the primary key in the database. The remaining ids are carried
 * only for external links and for resolving legacy backups.
 */
@Parcelize
data class Ids(
  val tmdb: IdTmdb,
  val slug: IdSlug,
  val tvdb: IdTvdb,
  val imdb: IdImdb,
  val tvrage: IdTvRage,
) : Parcelable {

  companion object {
    val EMPTY = Ids(
      IdTmdb(),
      IdSlug(),
      IdTvdb(),
      IdImdb(),
      IdTvRage(),
    )
  }
}

sealed interface Id : Parcelable

@JvmInline
@Parcelize
value class IdTmdb(
  val id: Long = -1,
) : Id

@JvmInline
@Parcelize
value class IdTvdb(
  val id: Long = -1,
) : Id

@JvmInline
@Parcelize
value class IdImdb(
  val id: String = "",
) : Id

@JvmInline
@Parcelize
value class IdTvRage(
  val id: Long = -1,
) : Id

@JvmInline
@Parcelize
value class IdSlug(
  val id: String = "",
) : Id
