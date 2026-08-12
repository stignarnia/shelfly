package xyz.stignarnia.data_local.di

import android.content.Context
import androidx.room.Room
import xyz.stignarnia.data_local.database.AppDatabase
import xyz.stignarnia.data_local.database.migrations.DATABASE_NAME
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StorageModule {

  @Provides
  @Singleton
  internal fun providesDatabase(
    @ApplicationContext context: Context,
  ): AppDatabase {
    Timber.d("Creating database...")
    return Room
      .databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME,
      ).apply {
        // Version 42 re-keys every table onto TMDB ids. Rows written before
        // it used ids from the previous catalog source, which cannot be
        // converted without querying that source, so any older database is
        // discarded and the user restores from a backup file instead. That
        // makes the upstream migration chain for versions 1 to 41 unreachable,
        // so it is gone.
        fallbackToDestructiveMigration(dropAllTables = true)
      }.build()
  }

  @Provides
  @Singleton
  internal fun providesTransactions(database: AppDatabase): TransactionsProvider = TransactionsProvider(database)
}
