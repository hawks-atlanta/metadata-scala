package org.hawksatlanta.metadata
package files_metadata.infrastructure

import com.wix.accord.validate
import files_metadata.infrastructure.requests.CreationReqSchema
import shared.infrastructure.CommonValidator
import ujson.Obj
import upickle.default.read

object MetadataControllers {
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
      val validationResult = validate[CreationReqSchema]( decoded )(
        CreationReqSchema.schemaValidator
      )

      if (validationResult.isFailure) {
        val errorsList = validationResult.toFailure
          .map( failure => failure.violations.toList )
          .getOrElse( List.empty )

        return cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "Fields validation failed",
            "errors"  -> errorsList.map( _.toString )
          ),
          statusCode = 400
        )
      }

      // TODO: Save the metadata to the database
      cask.Response(
        ujson.Obj( "error" -> false, "message" -> "Working on it..." ),
        statusCode = 200
      )
    } catch {
      // Unable to parse the given JSON payload
      case e: upickle.core.AbortException =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "JSON payload wasn't valid"
          ),
          statusCode = 400
        )

      // Any other error
      case _: Exception =>
        cask.Response(
          ujson.Obj(
            "error"   -> true,
            "message" -> "An unexpected error occurred"
          ),
          statusCode = 500
        )
    }
  }
}
