package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

trait FilesMetaRepository {
  // --- Create ---
  def saveFileMeta( archiveMeta: ArchivesMeta, fileMeta: FileMeta ): UUID

  def shareFile( fileUUID: UUID, userUUID: UUID ): Unit

  // --- Read ---
  def getFilesMetaInRoot( ownerUuid: UUID ): Seq[FileMeta]

  def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: UUID
  ): Seq[FileMeta]

  def getFileMeta( uuid: UUID ): FileMeta

  def getArchiveMeta( uuid: UUID ): ArchivesMeta

  def getFilesSharedWithUserMeta( userUuid: UUID ): Seq[FileMeta]

  def searchFileInDirectory(
      ownerUuid: UUID,
      directoryUuid: Option[UUID],
      fileName: String
  ): Option[FileMeta]

  def isFileDirectlySharedWithUser(
      fileUuid: UUID,
      userUuid: UUID
  ): Boolean

  def canUserReadFile( userUuid: UUID, fileUuid: UUID ): Boolean

  // --- Update ---
  def updateArchiveStatus( archiveUUID: UUID, ready: Boolean ): Unit

  def updateFileVolume( fileUUID: UUID, volume: String ): Unit

  // --- Delete ---
  def deleteFileMeta( ownerUuid: UUID, uuid: UUID ): Unit
}
