package xyz.stignarnia.dataRemote.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.dataRemote.MainRemoteDataSource
import xyz.stignarnia.dataRemote.RemoteDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataModule {
  @Binds
  @Singleton
  internal abstract fun providesRemoteDataSource(source: MainRemoteDataSource): RemoteDataSource
}
