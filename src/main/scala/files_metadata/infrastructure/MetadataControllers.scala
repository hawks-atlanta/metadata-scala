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
      request: cask.Request,
      userUUID: String
  ): cask.Response[Obj] = {
    // Check if the given user UUID is valid
    val isUserUUIDValid = CommonValidator.validateUUID( userUUID )
    if (!isUserUUIDValid) {
      return cask.Response(
        ujson.Obj(
          "error"   -> true,
          "message" -> "The userUUID parameter wasn't a valid UUID"
        ),
        statusCode = 400
      )
    }

    try {
      // Decode the JSON payload
      val decoded: CreationReqSchema = read[CreationReqSchema](
        request.text()
      )

      // Validate the payload
      var validationRule: Validator[CreationReqSchema] = null

      if (decoded.fileType == "file")
        validationRule = CreationReqSchema.fileSchemaValidator
      else if (decoded.fileType == "directory")
        validationRule = CreationReqSchema.directorySchemaValidator
      else
        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "The fileType should be either 'file' or 'directory'"
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
        ownerUuid = UUID.fromString( userUUID ),
        parentUuid = parentUUID,
        name = decoded.fileName
      )

      // Save the metadata
      useCases.saveMetadata( receivedArchiveMeta, receivedFileMeta )
      cask.Response(
        ujson.Obj(
          "error"   -> false,
          "message" -> "Metadata was saved successfully"
        ),
        statusCode = 200
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

      case parentDirectoryNotFound: DomainExceptions.FileNoutFoundException =>
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
}
