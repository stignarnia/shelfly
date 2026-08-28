package xyz.stignarnia.uiStatistics.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem
import javax.inject.Inject

@ViewModelScoped
class StatisticsLoadRatingsCase
  @Inject
  constructor(
    private val showsRepository: ShowsRepository,
    private val ratingsRepository: RatingsRepository,
    private val imagesProvider: ShowImagesProvider,
  ) {
    companion object {
      private const val LIMIT = 25
    }

    suspend fun loadRatings(): List<StatisticsRatingItem> {
      val ratings = ratingsRepository.shows.loadShowsRatings()

      val ratingsIds = ratings.map { it.idTmdb }
      val myShows = showsRepository.myShows.loadAll(ratingsIds)

      return ratings
        .filter { rating -> myShows.any { it.tmdbId == rating.idTmdb.id } }
        .take(LIMIT)
        .map { rating ->
          val show = myShows.first { it.tmdbId == rating.idTmdb.id }
          StatisticsRatingItem(
            isLoading = false,
            show = show,
            image = imagesProvider.findCachedImage(show, ImageType.POSTER),
            rating = rating,
          )
        }.sortedByDescending { it.rating.ratedAt }
    }
  }
