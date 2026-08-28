package xyz.stignarnia.uiBackup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.stignarnia.uiBackup.features.export.workers.BackupExportWorker
import xyz.stignarnia.uiBackup.features.export.workers.DefaultBackupExportWorker
import xyz.stignarnia.uiBackup.features.imports.migrations.CatalogIdResolver
import xyz.stignarnia.uiBackup.features.imports.migrations.TmdbCatalogIdResolver
import xyz.stignarnia.uiBackup.features.imports.workers.BackupImportWorker
import xyz.stignarnia.uiBackup.features.imports.workers.DefaultBackupImportWorker

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
