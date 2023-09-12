package org.hawksatlanta.metadata
package files_metadata.infrastructure

import java.util.UUID

import com.wix.accord.validate
import com.wix.accord.Validator
import files_metadata.application.FilesMetaUseCases
import files_metadata.domain.ArchivesMeta
import files_metadata.domain.BaseDomainException
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository
import files_metadata.infrastructure.requests.CreationReqSchema
import files_metadata.infrastructure.requests.MarkAsReadyReqSchema
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
        decoded.hashSum,
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
      val savedUUID =
        useCases.saveMetadata( receivedArchiveMeta, receivedFileMeta )
      cask.Response(
        ujson.Obj(
          "error"   -> false,
          "message" -> "Metadata was saved successfully",
          "uuid"    -> savedUUID.toString()
        ),
        statusCode = 201
      )
    } catch {
      case _: upickle.core.AbortException =>
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
            "message" -> "There was an error while saving the metadata"
          ),
          statusCode = 500
        )

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
      case _: upickle.core.AbortException =>
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
            "message" -> "There was an error while sharing the file"
          ),
          statusCode = 500
        )
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
            "message" -> "There was an error while checking if the user can read the file"
          ),
          statusCode = 500
        )
    }
  }

  def MarkArchiveAsReadyController(
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
      case _: upickle.core.AbortException =>
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

      case e: Exception =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "There was an error while marking the file as ready"
          ),
          statusCode = 500
        )
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
        // Directories metadata (Null archiveUUID)
        cask.Response(
          ujson.Obj(
            "volume"      -> fileMeta.volume,
            "archiveUUID" -> ujson.Null // Needs to be a "custom" null value
          ),
          statusCode = 200
        )
      } else {
        // Archives metadata
        cask.Response(
          ujson.Obj(
            "volume"      -> fileMeta.volume,
            "archiveUUID" -> fileMeta.archiveUuid.get.toString()
          ),
          statusCode = 200
        )
      }
    } catch {
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
            "message" -> "There was an error while getting the file metadata"
          ),
          statusCode = 500
        )
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
      case _: upickle.core.AbortException =>
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

      case e: Exception =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "There was an error while marking the file as ready"
          ),
          statusCode = 500
        )
    }
  }
}
