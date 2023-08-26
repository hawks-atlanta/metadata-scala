package org.hawksatlanta.metadata
package files_metadata.domain

object DomainExceptions {
  case class FileAlreadyExistsException( message: String )
      extends Exception( message )

  case class ArchiveNotSavedException( message: String )
      extends Exception( message )

  case class FileNotSavedException( message: String )
      extends Exception( message )
}
