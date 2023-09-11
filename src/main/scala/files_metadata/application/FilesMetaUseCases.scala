package org.hawksatlanta.metadata
package files_metadata.application

import java.util.UUID

import files_metadata.domain.ArchivesMeta
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository

class FilesMetaUseCases {
  private var repository: FilesMetaRepository = _

  def this( repository: FilesMetaRepository ) {
    this()
    this.repository = repository
  }

  def saveMetadata( archiveMeta: ArchivesMeta, fileMeta: FileMeta ): UUID = {
    // Check if the file already exists
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
        ownerUuid = fileMeta.ownerUuid,
        uuid = fileMeta.parentUuid.get
      )
    }

    // Save the metadata
    repository.saveFileMeta( archiveMeta, fileMeta )
  }

  def shareFile(
      ownerUUID: UUID,
      fileUUID: UUID,
      otherUserUUID: UUID
  ): Unit = {
    val fileMeta = repository.getFileMeta( ownerUUID, fileUUID )

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
}
