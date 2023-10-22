package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

trait FilesMetaRepository {
  // --- Create ---
  def saveArchiveMeta( archiveMeta: ArchiveMeta, fileMeta: FileMeta ): UUID

  def saveDirectoryMeta( fileMeta: FileMeta ): UUID

  def shareFile( fileUUID: UUID, userUUID: UUID ): Unit

  // --- Read ---
  def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: Option[UUID]
  ): Seq[FileExtendedMeta]

  def getFileMeta( uuid: UUID ): FileMeta

  def getArchiveMeta( uuid: UUID ): ArchiveMeta

  def getFilesSharedWithUserMeta( userUuid: UUID ): Seq[FileExtendedMeta]

  def getUsersFileWasSharedWith( fileUuid: UUID ): Seq[UUID]

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
  def updateArchiveToReady( file: FileMeta, volume: String ): Unit

  def updateFileName( fileUUID: UUID, newName: String ): Unit

  def updateFileParent( fileUUID: UUID, parentUUID: Option[UUID] ): Unit
  def unShareFile( fileUUID: UUID, userUUID: UUID ): Unit

  // --- Delete ---
  def deleteFileMeta( uuid: UUID ): Unit
  def deleteDirectoryMeta( uuid: UUID ): Unit
}
