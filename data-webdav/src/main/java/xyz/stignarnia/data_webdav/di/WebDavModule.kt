package xyz.stignarnia.data_webdav.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import xyz.stignarnia.data_webdav.OkHttpWebDavClient
import xyz.stignarnia.data_webdav.WebDavClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WebDavModule {

  @Provides
  @Singleton
  @Named("webdavOkHttpClient")
  fun providesWebDavOkHttpClient(): OkHttpClient =
    OkHttpClient
      .Builder()
      // Backups are small, but a home server on the far end of a slow link is the normal case rather than the exception.
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()

  @Provides
  @Singleton
  fun providesWebDavClient(
    @Named("webdavOkHttpClient") okHttpClient: OkHttpClient,
  ): WebDavClient = OkHttpWebDavClient(okHttpClient, Dispatchers.IO)
}
