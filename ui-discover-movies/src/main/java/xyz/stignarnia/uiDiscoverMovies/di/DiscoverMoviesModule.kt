package xyz.stignarnia.uiDiscoverMovies.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.uiBase.utilities.extensions.isTablet
import xyz.stignarnia.uiDiscoverMovies.helpers.itemtype.ImageTypeProvider
import xyz.stignarnia.uiDiscoverMovies.helpers.itemtype.PhoneImageTypeProvider
import xyz.stignarnia.uiDiscoverMovies.helpers.itemtype.TabletImageTypeProvider

@Module
@InstallIn(SingletonComponent::class)
class DiscoverMoviesModule {
  @Provides
  internal fun providesItemTypeProvider(
    @ApplicationContext context: Context,
  ): ImageTypeProvider =
    if (context.isTablet()) {
      TabletImageTypeProvider()
    } else {
      PhoneImageTypeProvider()
    }
}
