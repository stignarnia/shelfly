package xyz.stignarnia.dataLocal.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.MainLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataModule {
  @Binds
  @Singleton
  internal abstract fun providesLocalDataSource(source: MainLocalDataSource): LocalDataSource
}
