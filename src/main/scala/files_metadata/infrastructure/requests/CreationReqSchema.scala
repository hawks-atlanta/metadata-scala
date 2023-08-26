package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator
import shared.infrastructure.CommonValidator

// --- Base class ---
case class CreationReqSchema(
    parentUUID: String,
    hashSum: String,
    fileType: String,
    fileName: String,
    fileSize: Long
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
        request.parentUUID
          .is( aNull ) // Can be empty if it's in the root directory
          .or(         // Otherwise should be a valid UUID
            request.parentUUID should matchRegex(
              CommonValidator.uuidRegex
            )
          )
        request.hashSum.has( size == 64 ) // SHA-256
        request.fileType.is( equalTo( "file" ) )
        request.fileName.is( notEmpty )
        request.fileName.has( size <= 128 )
        request.fileSize should be > 0L
      }
    }

  val directorySchemaValidator: Validator[CreationReqSchema] =
    validator[CreationReqSchema] { request =>
      {
        request.parentUUID
          .is( aNull )
          .or(
            request.parentUUID should matchRegex(
              CommonValidator.uuidRegex
            )
          )
        request.hashSum.is( empty ) // HashSum is not needed for a directory
        request.fileType.is( equalTo( "directory" ) )
        request.fileName.is( notEmpty )
        request.fileName.has( size <= 128 )
        request.fileSize.is( equalTo( 0L ) ) // File size is 0 for a directory
      }
    }
}
