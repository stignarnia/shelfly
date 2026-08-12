package xyz.stignarnia.data_remote.di.module

import xyz.stignarnia.data_remote.Config.OMDB_BASE_URL
import xyz.stignarnia.data_remote.Config.TMDB_BASE_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

  @Provides
  @Singleton
  @Named("retrofitTmdb")
  fun providesTmdbRetrofit(
    @Named("okHttpTmdb") okHttpClient: OkHttpClient,
    moshi: Moshi,
  ): Retrofit =
    Retrofit
      .Builder()
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .baseUrl(TMDB_BASE_URL)
      .build()

  @Provides
  @Singleton
  @Named("retrofitOmdb")
  fun providesOmdbRetrofit(
    @Named("okHttpOmdb") okHttpClient: OkHttpClient,
    moshi: Moshi,
  ): Retrofit =
    Retrofit
      .Builder()
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .baseUrl(OMDB_BASE_URL)
      .build()

  @Provides
  @Singleton
  fun providesMoshiFactory(moshi: Moshi): MoshiConverterFactory =
    MoshiConverterFactory
      .create(moshi)

  @Provides
  @Singleton
  fun providesMoshi(): Moshi =
    Moshi
      .Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
}
