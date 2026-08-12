package xyz.stignarnia.ui_backup.di

import xyz.stignarnia.ui_backup.features.export.workers.BackupExportWorker
import xyz.stignarnia.ui_backup.features.export.workers.DefaultBackupExportWorker
import xyz.stignarnia.ui_backup.features.import_.migrations.CatalogIdResolver
import xyz.stignarnia.ui_backup.features.import_.migrations.TmdbCatalogIdResolver
import xyz.stignarnia.ui_backup.features.import_.workers.BackupImportWorker
import xyz.stignarnia.ui_backup.features.import_.workers.DefaultBackupImportWorker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupBindingModule {

  @Binds
  abstract fun bindExportWorker(worker: DefaultBackupExportWorker): BackupExportWorker

  @Binds
  abstract fun bindImportWorker(worker: DefaultBackupImportWorker): BackupImportWorker

  @Binds
  abstract fun bindCatalogIdResolver(resolver: TmdbCatalogIdResolver): CatalogIdResolver
}
