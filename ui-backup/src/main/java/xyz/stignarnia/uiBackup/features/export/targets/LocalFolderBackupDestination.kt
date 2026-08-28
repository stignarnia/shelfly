package xyz.stignarnia.uiBackup.features.export.targets

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import xyz.stignarnia.uiBackup.features.export.BackupFileName
import xyz.stignarnia.uiBackup.features.export.cases.ReadBackupJsonFromFileUseCase
import xyz.stignarnia.uiBackup.features.export.cases.WriteBackupJsonToFileUseCase

/**
 * Writes into a folder the user picked through the system file picker.
 *
 * Everything goes through the Storage Access Framework, which is why the app needs a persisted tree permission rather than a path: the folder may live on external storage or in another app's provider.
 */
internal class LocalFolderBackupDestination(
  private val context: Context,
  private val directoryUri: Uri,
  private val writeBackupJsonToFileUseCase: WriteBackupJsonToFileUseCase,
  private val readBackupJsonFromFileUseCase: ReadBackupJsonFromFileUseCase,
) : BackupDestination {
  /** The URI SAF minted for each write, so the read-back can find the file again. */
  private val createdUris = mutableMapOf<String, Uri>()

  private val childDocumentsUri: Uri
    get() {
      val treeDocumentId = DocumentsContract.getTreeDocumentId(directoryUri)
      return DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, treeDocumentId)
    }

  /**
   * SAF creates the document and hands back its URI, so the file has to exist before anything can be written into it.
   */
  override suspend fun write(
    fileName: String,
    content: String,
  ): Result<Unit> =
    runCatching {
      val fileUri =
        DocumentsContract.createDocument(
          context.contentResolver,
          childDocumentsUri,
          BackupFileName.memeType,
          fileName,
        ) ?: throw IllegalStateException("Could not create $fileName in the backup folder.")

      createdUris[fileName] = fileUri
      writeBackupJsonToFileUseCase(context, fileUri, content).getOrThrow()
    }

  override suspend fun read(fileName: String): Result<String> =
    runCatching {
      val fileUri =
        createdUris[fileName]
          ?: findUri(fileName)
          ?: throw IllegalStateException("Could not find $fileName in the backup folder.")
      readBackupJsonFromFileUseCase(context, fileUri).getOrThrow()
    }

  override suspend fun list(): Result<List<BackupEntry>> =
    runCatching {
      val projection =
        arrayOf(
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

      val entries = mutableListOf<BackupEntry>()
      context.contentResolver.query(childDocumentsUri, projection, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
          val documentId =
            cursor.getString(
              cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            )
          val name =
            cursor.getString(
              cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            )
          val lastModified =
            cursor.getLong(
              cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            )
          entries.add(
            BackupEntry(
              name = name,
              lastModifiedMillis = lastModified,
              handle = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId),
            ),
          )
        }
      }
      entries
    }

  override suspend fun delete(entry: BackupEntry): Result<Unit> =
    runCatching {
      val uri = entry.handle as? Uri ?: throw IllegalArgumentException("Missing document uri for ${entry.name}")
      if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
        throw IllegalStateException("Could not delete ${entry.name}")
      }
    }

  private suspend fun findUri(fileName: String): Uri? =
    list()
      .getOrNull()
      ?.firstOrNull { it.name == fileName }
      ?.handle as? Uri
}
