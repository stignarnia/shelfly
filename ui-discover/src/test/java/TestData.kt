import xyz.stignarnia.uiDiscover.recycler.DiscoverListItem
import xyz.stignarnia.uiModel.AirTime
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdSlug
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.IdTvRage
import xyz.stignarnia.uiModel.IdTvdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageFamily
import xyz.stignarnia.uiModel.ImageSource
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.ShowStatus

object TestData {
  val DISCOVER_LIST_ITEM =
    DiscoverListItem(
      show =
        Show(
          ids =
            Ids(
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
      image =
        Image(
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
