package org.hawksatlanta.metadata
package files_metadata.domain

abstract class BaseDomainException extends Exception {
  private var _statusCode: Int = _
  private var _message: String = _

  def this( message: String, statusCode: Int ) {
    this()
    _statusCode = statusCode
    _message = message
  }

  def statusCode: Int = _statusCode
  def message: String = _message
}

object DomainExceptions {
  case class FileNotFoundException( override val message: String )
      extends BaseDomainException( message, 404 )

  case class FileAlreadyExistsException( override val message: String )
      extends BaseDomainException( message, 409 )

  case class FileNotOwnedException( override val message: String )
      extends BaseDomainException( message, 403 )

  case class ArchiveNotSavedException( override val message: String )
      extends BaseDomainException( message, 500 )

  case class FileNotSavedException( override val message: String )
      extends BaseDomainException( message, 500 )

  case class FileAlreadySharedException( override val message: String )
      extends BaseDomainException( message, 409 )

  case class FileAlreadyMarkedAsReadyException( override val message: String )
      extends BaseDomainException( message, 409 )
}
