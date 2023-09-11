package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator
import shared.infrastructure.CommonValidator

case class ShareReqSchema(
    otherUserUUID: String
)

object ShareReqSchema {
  // --- Automatic JSON (de)serialization ---
  import upickle.default._

  implicit def rw: ReadWriter[ShareReqSchema] =
    macroRW[ShareReqSchema]

  // --- Validation ---
  val shareSchemaValidator: Validator[ShareReqSchema] =
    validator[ShareReqSchema] { request =>
      {
        request.otherUserUUID should matchRegex(
          CommonValidator.uuidRegex
        )
      }
    }
}
