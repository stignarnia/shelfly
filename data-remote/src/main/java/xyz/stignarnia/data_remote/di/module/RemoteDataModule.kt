package xyz.stignarnia.data_remote.di.module

import xyz.stignarnia.data_remote.MainRemoteDataSource
import xyz.stignarnia.data_remote.RemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataModule {

  @Binds
  @Singleton
  internal abstract fun providesRemoteDataSource(source: MainRemoteDataSource): RemoteDataSource
}
