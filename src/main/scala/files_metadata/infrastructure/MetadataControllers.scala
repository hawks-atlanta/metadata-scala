package org.hawksatlanta.metadata
package files_metadata.infrastructure

import java.util.UUID

import com.wix.accord.validate
import com.wix.accord.Validator
import files_metadata.application.FilesMetaUseCases
import files_metadata.domain.ArchivesMeta
import files_metadata.domain.BaseDomainException
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository
import files_metadata.infrastructure.requests.CreationReqSchema
import files_metadata.infrastructure.requests.MarkAsReadyReqSchema
import files_metadata.infrastructure.requests.MoveReqSchema
import files_metadata.infrastructure.requests.RenameReqSchema
import files_metadata.infrastructure.requests.ShareReqSchema
import shared.infrastructure.CommonValidator
import ujson.Obj
import upickle.default.read

class MetadataControllers {
  private var useCases: FilesMetaUseCases = _

  def _init(): Unit = {
    val repository: FilesMetaRepository =
      new FilesMetaPostgresRepository()

    useCases = new FilesMetaUseCases( repository )
  }

  private def _handleException( exception: Exception ): cask.Response[Obj] = {
    exception match {
      case _: upickle.core.AbortException | _: ujson.IncompleteParseException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Unable to decode JSON body"
          ),
          statusCode = 400
        )

      case e: BaseDomainException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> e.message
          ),
          statusCode = e.statusCode
        )

      case _: Exception =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "There was an error"
          ),
          statusCode = 500
        )
    }
  }

  def SaveMetadataController(
      request: cask.Request
  ): cask.Response[Obj] = {
    try {
      // Decode the JSON payload
      val decoded: CreationReqSchema = read[CreationReqSchema](
        request.text()
      )

      // Validate the payload
      var validationRule: Validator[CreationReqSchema] = null

      if (decoded.fileType == "archive")
        validationRule = CreationReqSchema.fileSchemaValidator
      else if (decoded.fileType == "directory")
        validationRule = CreationReqSchema.directorySchemaValidator
      else
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The fileType should be either 'archive' or 'directory'"
          ),
          statusCode = 400
        )

      val validationResult = validate[CreationReqSchema]( decoded )(
        validationRule
      )

      if (validationResult.isFailure) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      // Save the metadata
      val receivedArchiveMeta = ArchivesMeta.createNewArchive(
        decoded.fileExtension,
        decoded.fileSize
      )

      val parentUUID =
        if (decoded.parentUUID == null) None
        else Some( UUID.fromString( decoded.parentUUID ) )

      val receivedFileMeta = FileMeta.createNewFile(
        ownerUuid = UUID.fromString( decoded.userUUID ),
        parentUuid = parentUUID,
        name = decoded.fileName
      )

      // Save the metadata
      var savedUUID: UUID = null
      if (decoded.fileType == "archive") {
        savedUUID = useCases.saveArchiveMetadata(
          archiveMeta = receivedArchiveMeta,
          fileMeta = receivedFileMeta
        )
      } else {
        savedUUID = useCases.saveDirectoryMetadata(
          fileMeta = receivedFileMeta
        )
      }

      cask.Response(
        ujson.Obj(
          "error"   -> false,
          "message" -> "Metadata was saved successfully",
          "uuid"    -> savedUUID.toString()
        ),
        statusCode = 201
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def ShareFileController(
      request: cask.Request,
      ownerUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val decoded: ShareReqSchema = read[ShareReqSchema](
        request.text()
      )

      val isOwnerUUIDValid = CommonValidator.validateUUID( ownerUUID )
      val isFileUUIDValid  = CommonValidator.validateUUID( fileUUID )

      val validationRule: Validator[ShareReqSchema] =
        ShareReqSchema.shareSchemaValidator
      val validationResult = validate[ShareReqSchema]( decoded )(
        validationRule
      )
      if (!isOwnerUUIDValid || !isFileUUIDValid || validationResult.isFailure) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      useCases.shareFile(
        ownerUUID = UUID.fromString( ownerUUID ),
        fileUUID = UUID.fromString( fileUUID ),
        otherUserUUID = UUID.fromString( decoded.otherUserUUID )
      )

      cask.Response(
        None,
        statusCode = 204
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def CanReadFileController(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isUserUUIDValid = CommonValidator.validateUUID( userUUID )
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      if (!isUserUUIDValid || !isFileUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val canRead = useCases.canReadFile(
        userUUID = UUID.fromString( userUUID ),
        fileUUID = UUID.fromString( fileUUID )
      )

      if (!canRead) {
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The user can't read the file"
          ),
          statusCode = 403
        )
      } else {
        cask.Response( None, statusCode = 204 )
      }
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def GetFileMetadataController(
      request: cask.Request,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      if (!isFileUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val fileMeta = useCases.getFileMetadata(
        fileUUID = UUID.fromString( fileUUID )
      )

      if (fileMeta.volume == null) {
        return cask.Response(
          ujson.Obj(
            "message" -> "The file is not ready yet"
          ),
          statusCode = 202
        )
      }

      if (fileMeta.archiveUuid.isEmpty) {
        // Directories metadata
        cask.Response(
          ujson.Obj(
            "archiveUUID" -> ujson.Null, // Needs to be a "custom" null value
            "name"        -> fileMeta.name,
            "extension"   -> ujson.Null,
            "volume"      -> fileMeta.volume,
            "size"        -> 0
          ),
          statusCode = 200
        )
      } else {
        // Archives metadata
        val archivesMeta = useCases.getArchiveMetadata(
          archiveUUID = fileMeta.archiveUuid.get
        )

        cask.Response(
          ujson.Obj(
            "archiveUUID" -> fileMeta.archiveUuid.get.toString,
            "name"        -> fileMeta.name,
            "extension"   -> archivesMeta.extension,
            "volume"      -> fileMeta.volume,
            "size"        -> archivesMeta.size
          ),
          statusCode = 200
        )
      }
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def MarkFileAsReadyController(
      request: cask.Request,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      if (!isFileUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val decoded: MarkAsReadyReqSchema = read[MarkAsReadyReqSchema](
        request.text()
      )

      val validationRule: Validator[MarkAsReadyReqSchema] =
        MarkAsReadyReqSchema.schemaValidator
      val validationResult = validate[MarkAsReadyReqSchema]( decoded )(
        validationRule
      )
      if (validationResult.isFailure) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      useCases.updateSavedFile(
        fileUUID = UUID.fromString( fileUUID ),
        volume = decoded.volume
      )

      cask.Response(
        None,
        statusCode = 204
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def GetSharedWithMeController(
      request: cask.Request,
      userUUID: String
  ): cask.Response[Obj] = {
    try {
      val isUserUUIDValid = CommonValidator.validateUUID( userUUID )
      if (!isUserUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val filesMeta = useCases.getFilesMetadataSharedWithUser(
        userUUID = UUID.fromString( userUUID )
      )

      val responseArray = ujson.Arr.from(
        filesMeta.map( fileMeta => {
          if (fileMeta.archiveUuid.isEmpty) {
            ujson.Obj(
              "uuid"      -> fileMeta.uuid.toString,
              "fileType"  -> "directory",
              "name"      -> fileMeta.name,
              "extension" -> ujson.Null
            )
          } else {
            ujson.Obj(
              "uuid"      -> fileMeta.uuid.toString,
              "fileType"  -> "archive",
              "name"      -> fileMeta.name,
              "extension" -> fileMeta.extension
            )
          }
        } )
      )

      cask.Response(
        ujson.Obj(
          "files" -> responseArray
        ),
        statusCode = 200
      )

    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def GetSharedWithWhoController(
      request: cask.Request,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      if (!isFileUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val usersUUID = useCases.getUsersFileWasSharedWith(
        fileUUID = UUID.fromString( fileUUID )
      )

      val responseArray = ujson.Arr.from(
        usersUUID.map( userUUID => userUUID.toString )
      )

      cask.Response(
        ujson.Obj(
          "shared_with" -> responseArray
        ),
        statusCode = 200
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def RenameFileController(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      val isUserUUIDValid = CommonValidator.validateUUID( userUUID )
      if (!isFileUUIDValid || !isUserUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val decoded: RenameReqSchema = read[RenameReqSchema](
        request.text()
      )

      val validationRule: Validator[RenameReqSchema] =
        RenameReqSchema.schemaValidator
      val validationResult = validate[RenameReqSchema]( decoded )(
        validationRule
      )
      if (validationResult.isFailure) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      useCases.renameFile(
        fileUUID = UUID.fromString( fileUUID ),
        userUUID = UUID.fromString( userUUID ),
        newName = decoded.name
      )

      cask.Response(
        None,
        statusCode = 204
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }

  def MoveFileController(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    try {
      val isFileUUIDValid = CommonValidator.validateUUID( fileUUID )
      val isUserUUIDValid = CommonValidator.validateUUID( userUUID )
      if (!isFileUUIDValid || !isUserUUIDValid) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val decoded: MoveReqSchema = read[MoveReqSchema](
        request.text()
      )

      val validationRule: Validator[MoveReqSchema] =
        MoveReqSchema.schemaValidator
      val validationResult = validate[MoveReqSchema]( decoded )(
        validationRule
      )
      if (validationResult.isFailure) {
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed"
          ),
          statusCode = 400
        )
      }

      val parentUUID =
        if (decoded.parentUUID == null) None
        else Some( UUID.fromString( decoded.parentUUID ) )

      useCases.moveFile(
        userUUID = UUID.fromString( userUUID ),
        fileUUID = UUID.fromString( fileUUID ),
        newParentUUID = parentUUID
      )

      cask.Response(
        None,
        statusCode = 204
      )
    } catch {
      case e: Exception => _handleException( e )
    }
  }
}
