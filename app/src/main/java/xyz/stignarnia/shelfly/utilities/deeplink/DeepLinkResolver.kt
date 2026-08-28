package xyz.stignarnia.shelfly.utilities.deeplink

import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.shelfly.utilities.deeplink.resolvers.ImdbSourceResolver
import xyz.stignarnia.shelfly.utilities.deeplink.resolvers.TmdbSourceResolver
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkResolver
  @Inject
  constructor() {
    companion object {
      const val TMDB_TYPE_TV = "tv"
      const val TMDB_TYPE_MOVIE = "movie"
    }

    private val sourceResolvers =
      setOf(
        ImdbSourceResolver(),
        TmdbSourceResolver(),
      )

    fun findSource(intent: Intent?): DeepLinkSource? {
      val path = intent?.data?.pathSegments ?: emptyList()
      return sourceResolvers.firstNotNullOfOrNull { it.resolve(path) }
    }

    /**
     * Details are a top level destination, so the link opens over whatever the user was looking at.
     * Nothing is unwound to get there: the link is a step forward, and back still leads where the user came from.
     */
    fun resolveDestination(
      navController: NavController,
      show: Show,
    ) {
      navController.navigateSafe(
        R.id.actionNavigateShowDetailsFragment,
        Bundle().apply { putLong(NavigationArgs.ARG_SHOW_ID, show.tmdbId) },
      )
    }

    fun resolveDestination(
      navController: NavController,
      movie: Movie,
    ) {
      navController.navigateSafe(
        R.id.actionNavigateMovieDetailsFragment,
        Bundle().apply { putLong(NavigationArgs.ARG_MOVIE_ID, movie.tmdbId) },
      )
    }

    /** A link arrives whenever it arrives, including at a moment the fragment manager will not take a transaction. */
    private fun NavController.navigateSafe(
      actionId: Int,
      args: Bundle,
    ) {
      try {
        navigate(actionId, args)
      } catch (error: Throwable) {
      }
    }
  }
