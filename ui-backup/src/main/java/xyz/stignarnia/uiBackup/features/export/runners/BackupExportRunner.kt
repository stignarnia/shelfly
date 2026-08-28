package xyz.stignarnia.uiBackup.features.export.runners

internal abstract class BackupExportRunner<T> {
  abstract suspend fun run(): T
}
