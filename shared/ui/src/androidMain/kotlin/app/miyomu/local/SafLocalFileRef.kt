package app.miyomu.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * SAF-backed [LocalFileRef] over a persisted tree URI (OpenDocumentTree). Android counterpart of
 * [FileLocalFileRef]; runtime behavior (persisted permission survival and scans) is verified on
 * device/emulator per ADR-0014's follow-up note.
 */
class SafLocalFileRef private constructor(
    private val context: Context,
    private val treeUri: Uri,
    private val documentId: String,
    override val name: String,
    override val isDirectory: Boolean,
    override val size: Long,
    override val lastModified: Long,
) : LocalFileRef {

    override val location: String =
        treeUri.buildUpon().appendQueryParameter("doc", documentId).toString()

    private val documentUri: Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    override fun list(): List<LocalFileRef> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val children = mutableListOf<LocalFileRef>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                children += SafLocalFileRef(
                    context = context,
                    treeUri = treeUri,
                    documentId = id,
                    name = cursor.getString(1) ?: id,
                    isDirectory = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                    size = cursor.getLong(3),
                    lastModified = cursor.getLong(4),
                )
            }
        }
        return children
    }

    override fun readBytes(): ByteArray =
        context.contentResolver.openInputStream(documentUri)?.use { it.readBytes() }
            ?: error("SAF 条目无法作为文件读取：$location")

    override fun delete(): Boolean = runCatching {
        DocumentsContract.deleteDocument(context.contentResolver, documentUri)
    }.getOrDefault(false)

    companion object {

        /** Builds the root ref for a persisted tree URI; the tree itself is a directory. */
        fun forTree(context: Context, treeUri: Uri): SafLocalFileRef {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            return SafLocalFileRef(
                context = context,
                treeUri = treeUri,
                documentId = documentId,
                name = documentId.substringAfter(':').ifEmpty { "SAF 库目录" },
                isDirectory = true,
                size = 0L,
                lastModified = 0L,
            )
        }

        /** True when the persisted tree still resolves (permission survived the reboot). */
        fun isAccessible(context: Context, treeUri: Uri): Boolean = runCatching {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }
}
