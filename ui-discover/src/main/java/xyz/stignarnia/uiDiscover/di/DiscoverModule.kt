package xyz.stignarnia.uiDiscover.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.uiBase.utilities.extensions.isTablet
import xyz.stignarnia.uiDiscover.helpers.itemtype.ImageTypeProvider
import xyz.stignarnia.uiDiscover.helpers.itemtype.PhoneImageTypeProvider
import xyz.stignarnia.uiDiscover.helpers.itemtype.TabletImageTypeProvider

@Module
@InstallIn(SingletonComponent::class)
class DiscoverModule {
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
