import xyz.stignarnia.ui_discover.recycler.DiscoverListItem
import xyz.stignarnia.ui_model.AirTime
import xyz.stignarnia.ui_model.IdImdb
import xyz.stignarnia.ui_model.IdSlug
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.IdTvRage
import xyz.stignarnia.ui_model.IdTvdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageFamily
import xyz.stignarnia.ui_model.ImageSource
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.ShowStatus

object TestData {

  val DISCOVER_LIST_ITEM = DiscoverListItem(
    show = Show(
      ids = Ids(
        slug = IdSlug(id = ""),
        tvdb = IdTvdb(id = 0),
        imdb = IdImdb(id = ""),
        tmdb = IdTmdb(id = 0),
        tvrage = IdTvRage(id = 0),
      ),
      title = "DISCOVER_LIST_ITEM",
      year = 0,
      overview = "",
      firstAired = "",
      runtime = 0,
      airTime = AirTime(day = "", time = "", timezone = ""),
      certification = "",
      network = "",
      country = "",
      trailer = "",
      homepage = "",
      status = ShowStatus.UNKNOWN,
      rating = 0.0f,
      votes = 0,
      commentCount = 0,
      genres = listOf(),
      airedEpisodes = 0,
      createdAt = 0,
      updatedAt = 0,
    ),
    image = Image(
      id = 0,
      idTvdb = IdTvdb(id = 0),
      idTmdb = IdTmdb(id = 0),
      type = ImageType.POSTER,
      family = ImageFamily.SHOW,
      fileUrl = "",
      thumbnailUrl = "",
      status = ImageStatus.UNKNOWN,
      source = ImageSource.TMDB,
    ),
    isLoading = false,
    isFollowed = false,
    isWatchlist = false,
  )
}
