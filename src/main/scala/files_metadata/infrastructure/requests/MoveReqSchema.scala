package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator
import shared.infrastructure.CommonValidator

case class MoveReqSchema(
    parentUUID: String
)

object MoveReqSchema {
  import upickle.default._
  implicit def rw: ReadWriter[MoveReqSchema] =
    macroRW[MoveReqSchema]

  val schemaValidator: Validator[MoveReqSchema] =
    validator[MoveReqSchema] { req =>
      req.parentUUID
        .is( aNull )
        .or( req.parentUUID.should( matchRegex( CommonValidator.uuidRegex ) ) )
    }
}
