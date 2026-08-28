package xyz.stignarnia.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.dispatchers.DefaultCoroutineDispatchers

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonBindingModule {
  @Binds
  abstract fun bindCoroutineDispatchers(dispatchers: DefaultCoroutineDispatchers): CoroutineDispatchers
}
