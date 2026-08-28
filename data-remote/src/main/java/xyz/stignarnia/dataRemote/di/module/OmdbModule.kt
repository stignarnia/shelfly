package xyz.stignarnia.dataRemote.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.dataRemote.omdb.OmdbInterceptor
import xyz.stignarnia.dataRemote.omdb.OmdbRemoteDataSource
import xyz.stignarnia.dataRemote.omdb.api.OmdbApi
import xyz.stignarnia.dataRemote.omdb.api.OmdbService
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OmdbModule {
  @Provides
  @Singleton
  fun providesOmdbApi(
    @Named("retrofitOmdb") retrofit: Retrofit,
  ): OmdbRemoteDataSource = OmdbApi(retrofit.create(OmdbService::class.java))

  @Provides
  @Singleton
  fun providesOmdbInterceptor(apiKeyProvider: ApiKeyProvider) = OmdbInterceptor(apiKeyProvider)
}
