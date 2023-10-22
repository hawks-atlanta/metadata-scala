package org.hawksatlanta.metadata
package files_metadata.application

import java.util.UUID

import files_metadata.domain.ArchiveMeta
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileExtendedMeta
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository

class FilesMetaUseCases {
  private var repository: FilesMetaRepository = _

  def this( repository: FilesMetaRepository ) {
    this()
    this.repository = repository
  }

  private def ensureFileCanBeCreated(
      fileMeta: FileMeta
  ): Unit = {
    val existingFileMeta = repository.searchFileInDirectory(
      ownerUuid = fileMeta.ownerUuid,
      directoryUuid = fileMeta.parentUuid,
      fileName = fileMeta.name
    )

    if (existingFileMeta.isDefined) {
      throw DomainExceptions.FileAlreadyExistsException(
        "A file with the same name already exists in the given directory"
      )
    }

    // If a parent directory is given, check if it exists
    if (fileMeta.parentUuid.isDefined) {
      repository.getFileMeta(
        uuid = fileMeta.parentUuid.get
      )
    }
  }

  def listFiles(
      userUUID: UUID,
      parentUUID: Option[UUID]
  ): Seq[FileExtendedMeta] = {
    if (parentUUID.isDefined) {
      // Check the parent exists
      val parentMeta = repository.getFileMeta( parentUUID.get )

      // Check the parent is a directory
      val parentIsDirectory = parentMeta.archiveUuid.isEmpty
      if (!parentIsDirectory) {
        throw DomainExceptions.ParentIsNotADirectoryException(
          "The parent is not a directory"
        )
      }

      // Check the user has access to the parent
      if (!canReadFile( userUUID, parentUUID.get )) {
        throw DomainExceptions.CannotReadFileException(
          "You do not have access to the parent directory"
        )
      }
    }

    repository.getFilesMetaInDirectory( userUUID, parentUUID )
  }

  def saveArchiveMetadata(
      archiveMeta: ArchiveMeta,
      fileMeta: FileMeta
  ): UUID = {
    ensureFileCanBeCreated( fileMeta = fileMeta )
    repository.saveArchiveMeta( archiveMeta, fileMeta )
  }

  def saveDirectoryMetadata(
      fileMeta: FileMeta
  ): UUID = {
    ensureFileCanBeCreated( fileMeta = fileMeta )
    repository.saveDirectoryMeta( fileMeta )
  }

  def shareFile(
      ownerUUID: UUID,
      fileUUID: UUID,
      otherUserUUID: UUID
  ): Unit = {
    val fileMeta = repository.getFileMeta( fileUUID )

    if (fileMeta.ownerUuid != ownerUUID) {
      throw DomainExceptions.FileNotOwnedException(
        "The user does not own the file"
      )
    }

    if (
      fileMeta.ownerUuid == otherUserUUID ||
      repository.isFileDirectlySharedWithUser( fileUUID, otherUserUUID )
    ) {
      throw DomainExceptions.FileAlreadySharedException(
        "The file is already shared with the user"
      )
    }

    repository.shareFile( fileUUID, otherUserUUID )
  }

  def canReadFile(
      userUUID: UUID,
      fileUUID: UUID
  ): Boolean = {
    val fileMeta = repository.getFileMeta( fileUUID )
    if (fileMeta.ownerUuid == userUUID) return true
    repository.canUserReadFile( userUUID, fileUUID )
  }

  def updateSavedFile( fileUUID: UUID, volume: String ): Unit = {
    val fileMetadata = repository.getFileMeta( fileUUID )

    // Skip if the file is a directory
    val fileIsDirectory = fileMetadata.archiveUuid.isEmpty
    if (fileIsDirectory) {
      throw DomainExceptions.FileAlreadyMarkedAsReadyException(
        "Directories cannot be marked as ready"
      )
    }

    // Skip if the file is already marked as ready
    if (fileMetadata.volume != null) {
      throw DomainExceptions.FileAlreadyMarkedAsReadyException(
        "The file was already marked as ready"
      )
    }

    // Update the status and volume
    repository.updateArchiveToReady( fileMetadata, volume )
  }

  def getFileMetadata( fileUUID: UUID ): FileMeta = {
    repository.getFileMeta( fileUUID )
  }

  def getArchiveMetadata( archiveUUID: UUID ): ArchiveMeta = {
    repository.getArchiveMeta( archiveUUID )
  }

  def getFilesMetadataSharedWithUser(
      userUUID: UUID
  ): Seq[FileExtendedMeta] = {
    repository.getFilesSharedWithUserMeta( userUUID )
  }

  def getUsersFileWasSharedWith( fileUUID: UUID ): Seq[UUID] = {
    repository.getFileMeta( fileUUID )
    repository.getUsersFileWasSharedWith( fileUUID )
  }

  def renameFile( userUUID: UUID, fileUUID: UUID, newName: String ): Unit = {
    val fileMeta = repository.getFileMeta( fileUUID )
    if (fileMeta.ownerUuid != userUUID) {
      throw DomainExceptions.FileNotOwnedException(
        "The user does not own the file"
      )
    }

    val existingFileMeta = repository.searchFileInDirectory(
      ownerUuid = fileMeta.ownerUuid,
      directoryUuid = fileMeta.parentUuid,
      fileName = newName
    )
    if (existingFileMeta.isDefined) {
      throw DomainExceptions.FileAlreadyExistsException(
        "A file with the same name already exists in the file directory"
      )
    }

    repository.updateFileName( fileUUID, newName )
  }

  def moveFile(
      userUUID: UUID,
      fileUUID: UUID,
      newParentUUID: Option[UUID]
  ): Unit = {
    // Check the file exists
    val fileMeta = repository.getFileMeta( fileUUID )

    // Check the user owns the file
    if (fileMeta.ownerUuid != userUUID) {
      throw DomainExceptions.FileNotOwnedException(
        "The user does not own the file"
      )
    }

    // Check the current parent is not the same as the new parent
    if (fileMeta.parentUuid.orNull == newParentUUID.orNull) {
      throw DomainExceptions.FileAlreadyExistsException(
        "The file is already in the given directory"
      )
    }

    if (newParentUUID.isDefined) {
      // Check the parent exists
      val newParentMeta = repository.getFileMeta( newParentUUID.get )

      // Check the new parent is a directory
      val parentIsDirectory = newParentMeta.archiveUuid.isEmpty
      if (!parentIsDirectory) {
        throw DomainExceptions.ParentIsNotADirectoryException(
          "The new parent is not a directory"
        )
      }
    }

    // Check there is no file with the same name in the new parent
    val existingFileMeta = repository.searchFileInDirectory(
      ownerUuid = fileMeta.ownerUuid,
      directoryUuid = newParentUUID,
      fileName = fileMeta.name
    )
    if (existingFileMeta.isDefined) {
      throw DomainExceptions.FileAlreadyExistsException(
        "A file with the same name already exists in the file directory"
      )
    }

    repository.updateFileParent( fileUUID, newParentUUID )
  }

  def unShareFile(
      ownerUUID: UUID,
      fileUUID: UUID,
      otherUserUUID: UUID
  ): Unit = {
    val fileMeta = repository.getFileMeta( fileUUID )

    if (fileMeta.ownerUuid != ownerUUID) {
      throw DomainExceptions.FileNotOwnedException(
        "You don't own the file"
      )
    }
    if (ownerUUID == otherUserUUID) {
      throw DomainExceptions.FileNotOwnedException(
        "You cannot un-share a file with yourself"
      )
    }

    if (!repository.isFileDirectlySharedWithUser( fileUUID, otherUserUUID )) {
      throw DomainExceptions.FileAlreadySharedException(
        "The file is not shared with the given user"
      )
    }

    repository.unShareFile( fileUUID, otherUserUUID )
  }
}
