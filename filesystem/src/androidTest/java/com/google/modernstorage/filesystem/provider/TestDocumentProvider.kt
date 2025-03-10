/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.modernstorage.filesystem.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.FileNotFoundException
import java.io.FileWriter
import kotlin.concurrent.thread

class TestDocumentProvider : DocumentsProvider() {

    companion object {
        private val testRoots = mutableListOf<TestDocument>()
        private val docIdsToDoc = mutableMapOf<String, TestDocument>()

        fun clearAll() {
            testRoots.clear()
            docIdsToDoc.clear()
        }

        fun addRoot(root: TestDocument) {
            if (root !in testRoots) {
                testRoots += root
                updateMap(null, root)
            }
        }

        private fun updateMap(root: String?, doc: TestDocument) {
            val fullDocId = if (root != null) "$root/${doc.docId}" else doc.docId
            if (docIdsToDoc[fullDocId] == null) {
                docIdsToDoc[fullDocId] = doc
            }
            if (doc.children?.isNotEmpty() == true) {
                doc.children.forEach { childDocument ->
                    updateMap(fullDocId, childDocument)
                }
            }
        }
    }

    private val defaultRootProjection = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_FLAGS,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_TITLE,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES
    )

    private val defaultDocumentProjection = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE
    )

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val useProjection = projection ?: defaultRootProjection
        val cursor = MatrixCursor(useProjection)

        testRoots.forEach { root ->
            // Add row for the root document/directory
            cursor.newRow().apply {
                add(Root.COLUMN_ROOT_ID, root.docId)
                add(Root.COLUMN_SUMMARY, "")
                add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD)
                add(Root.COLUMN_TITLE, "TestProvider")
                add(Root.COLUMN_DOCUMENT_ID, root.docId)
                add(Root.COLUMN_MIME_TYPES, "*/*")
                add(Root.COLUMN_AVAILABLE_BYTES, null)
            }
        }

        return cursor
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val document = docIdsToDoc[documentId] ?: throw FileNotFoundException()

        val useProjection = projection ?: defaultDocumentProjection
        val cursor = MatrixCursor(useProjection)
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, document.docId)
            add(Document.COLUMN_MIME_TYPE, document.mimeType)
            add(Document.COLUMN_DISPLAY_NAME, document.displayName)
            add(Document.COLUMN_LAST_MODIFIED, 0)
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_SIZE, document.size)
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val parentDocument = docIdsToDoc[parentDocumentId] ?: throw FileNotFoundException()
        val useProjection = projection ?: defaultRootProjection

        val cursor = MatrixCursor(useProjection)
        parentDocument.children?.forEach { childDocument ->
            cursor.newRow().apply {
                val fullDocId = "$parentDocumentId/${childDocument.docId}"
                add(Document.COLUMN_DOCUMENT_ID, fullDocId)
                add(Document.COLUMN_MIME_TYPE, childDocument.mimeType)
                add(Document.COLUMN_DISPLAY_NAME, childDocument.displayName)
                add(Document.COLUMN_LAST_MODIFIED, 0)
                add(Document.COLUMN_FLAGS, 0)
                add(Document.COLUMN_SIZE, childDocument.size)
            }
        }

        return cursor
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        // The actual check can be multiple levels down, so root -> dirA -> dir2, and this would
        // ask, "is dir2 a child of root?" -- In a real system you'd probably want to actually
        // do this check, but here it's a lot of work for something that's good enough for
        // the test cases.
        return docIdsToDoc[documentId] != null
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val document = docIdsToDoc[documentId] ?: throw FileNotFoundException()
        val (readFd, writeFd) = ParcelFileDescriptor.createPipe()
        thread(name = "io") {
            FileWriter(writeFd.fileDescriptor).apply { write((document.content)) }
            writeFd.close()
        }
        return readFd
    }
}
