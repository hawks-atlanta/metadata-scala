package org.hawksatlanta.metadata
package files_metadata.application

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

  def saveMetadata( archiveMeta: ArchivesMeta, fileMeta: FileMeta ): Unit = {
    // Check if the file already exists
    val existingFileMeta = repository.searchFileInDirectory(
      ownerUuid = fileMeta.ownerUuid,
      directoryUuid = fileMeta.parentUuid,
      fileName = fileMeta.name
    )

    if (existingFileMeta.isDefined) {
      throw DomainExceptions.FileAlreadyExistsException(
        "The user already has a file with the same name in the given directory"
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
}
