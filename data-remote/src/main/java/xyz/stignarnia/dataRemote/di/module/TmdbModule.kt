package xyz.stignarnia.dataRemote.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.dataRemote.tmdb.TmdbInterceptor
import xyz.stignarnia.dataRemote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.dataRemote.tmdb.api.TmdbApi
import xyz.stignarnia.dataRemote.tmdb.api.TmdbService
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TmdbModule {
  @Provides
  @Singleton
  fun providesTmdbApi(
    @Named("retrofitTmdb") retrofit: Retrofit,
  ): TmdbRemoteDataSource = TmdbApi(retrofit.create(TmdbService::class.java))

  @Provides
  @Singleton
  fun providesTmdbInterceptor(apiKeyProvider: ApiKeyProvider) = TmdbInterceptor(apiKeyProvider)
}
