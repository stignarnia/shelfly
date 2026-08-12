package xyz.stignarnia.data_remote.di.module

import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import xyz.stignarnia.data_remote.tmdb.TmdbInterceptor
import xyz.stignarnia.data_remote.tmdb.TmdbRemoteDataSource
import xyz.stignarnia.data_remote.tmdb.api.TmdbApi
import xyz.stignarnia.data_remote.tmdb.api.TmdbService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
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
