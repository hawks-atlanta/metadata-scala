package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

trait FilesMetaRepository {
  // --- Create ---
  def saveFileMeta( archiveMeta: ArchivesMeta, fileMeta: FileMeta ): UUID

  // --- Read ---
  def getFilesMetaInRoot( ownerUuid: UUID ): Seq[FileMeta]

  def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: UUID
  ): Seq[FileMeta]

  def getFileMeta( ownerUuid: UUID, uuid: UUID ): FileMeta

  def searchFileInDirectory(
      ownerUuid: UUID,
      directoryUuid: Option[UUID],
      fileName: String
  ): Option[FileMeta]
  // --- Update ---
  def updateFileStatus( uuid: UUID, ready: Boolean ): Unit

  // --- Delete ---
  def deleteFileMeta( ownerUuid: UUID, uuid: UUID ): Unit
}
