package xyz.stignarnia.dataLocal.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import xyz.stignarnia.dataLocal.database.AppDatabase
import xyz.stignarnia.dataLocal.database.migrations.DATABASE_NAME
import xyz.stignarnia.dataLocal.database.migrations.MIGRATION_42_43
import xyz.stignarnia.dataLocal.database.migrations.MIGRATION_43_44
import xyz.stignarnia.dataLocal.database.migrations.MIGRATION_44_45
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
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
        // Version 42 re-keys every table onto TMDB ids.
        // Rows written before it used ids from the previous catalog source, which cannot be converted without querying that source, so any older database is discarded and the user restores from a backup file instead.
        // That makes the upstream migration chain for versions 1 to 41 unreachable, so it is gone.
        fallbackToDestructiveMigration(dropAllTables = true)
        // Everything from 42 onwards migrates properly - there is user data worth keeping now.
        addMigrations(MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45)
      }.build()
  }

  @Provides
  @Singleton
  internal fun providesTransactions(database: AppDatabase): TransactionsProvider = TransactionsProvider(database)
}
