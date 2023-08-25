package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator
import org.hawksatlanta.metadata.shared.infrastructure.CommonValidator

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
  val schemaValidator: Validator[CreationReqSchema] =
    validator[CreationReqSchema] { request =>
      {
        request.parentUUID should matchRegex( CommonValidator.uuidRegex )
        request.hashSum.has( size == 64 ) // SHA-256
        request.fileType should matchRegex( "^(file|directory)$" )
        request.fileName.is( notEmpty )
        request.fileName.has( size <= 128 )
        request.fileSize should be > 0L
      }
    }
}
