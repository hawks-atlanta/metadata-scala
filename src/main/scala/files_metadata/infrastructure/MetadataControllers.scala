package org.hawksatlanta.metadata
package files_metadata.infrastructure

import java.util.UUID

import com.wix.accord.validate
import com.wix.accord.Validator
import files_metadata.application.FilesMetaUseCases
import files_metadata.domain.ArchivesMeta
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository
import files_metadata.infrastructure.requests.CreationReqSchema
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
      // Unable to parse the given JSON payload
      case _: upickle.core.AbortException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "JSON payload wasn't valid"
          ),
          statusCode = 400
        )

      case conflict: DomainExceptions.FileAlreadyExistsException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> conflict.getMessage()
          ),
          statusCode = 409
        )

      case parentDirectoryNotFound: DomainExceptions.FileNotFoundException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> parentDirectoryNotFound.getMessage()
          ),
          statusCode = 404
        )

      // Any other error
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
            "message" -> "JSON payload wasn't valid"
          ),
          statusCode = 400
        )

      case _: DomainExceptions.FileNotFoundException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The file wasn't found"
          ),
          statusCode = 404
        )

      case _: DomainExceptions.FileNotOwnedException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The user does not own the file"
          ),
          statusCode = 403
        )

      case _: DomainExceptions.FileAlreadySharedException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The file is already shared with the given user"
          ),
          statusCode = 409
        )

      case e: Exception =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "There was an error while sharing the file"
          ),
          statusCode = 500
        )
    }
  }
}
