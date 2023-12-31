package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator
import shared.infrastructure.CommonValidator

// --- Base class ---
case class CreationReqSchema(
    userUUID: String,
    parentUUID: String,
    fileType: String,
    fileName: String,
    fileExtension: String,
    fileSize: Int
)

object CreationReqSchema {
  // --- Automatic JSON (de)serialization ---
  import upickle.default._

  implicit def rw: ReadWriter[CreationReqSchema] =
    macroRW[CreationReqSchema]

  // --- Validation ---
  val fileSchemaValidator: Validator[CreationReqSchema] =
    validator[CreationReqSchema] { request =>
      {
        request.userUUID should matchRegex(
          CommonValidator.uuidRegex
        )
        request.parentUUID
          .is( aNull ) // Can be empty if it's in the root directory
          .or(
            request.parentUUID should matchRegex(
              CommonValidator.uuidRegex
            )
          )
        request.fileType.is( equalTo( "archive" ) )
        request.fileName.is( notEmpty )
        request.fileExtension
          .is( aNull ) // Can't be able to recognize MIME type
          .or(
            request.fileExtension
              .is( notEmpty )
              .and( request.fileExtension.has( size <= 16 ) )
          )
        request.fileName.has( size <= 128 )
        request.fileSize should be > 0
      }
    }

  val directorySchemaValidator: Validator[CreationReqSchema] =
    validator[CreationReqSchema] { request =>
      {
        request.userUUID should matchRegex(
          CommonValidator.uuidRegex
        )
        request.parentUUID
          .is( aNull )
          .or(
            request.parentUUID should matchRegex(
              CommonValidator.uuidRegex
            )
          )
        request.fileType.is( equalTo( "directory" ) )
        request.fileName.is( notEmpty )
        request.fileName.has( size <= 128 )
        request.fileExtension.is( aNull )
        request.fileSize.is( equalTo( 0 ) ) // File size is 0 for a directory
      }
    }
}
