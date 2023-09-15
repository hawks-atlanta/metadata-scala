package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator

case class RenameReqSchema(
    name: String
)

object RenameReqSchema {
  import upickle.default._
  implicit def rw: ReadWriter[RenameReqSchema] =
    macroRW[RenameReqSchema]

  val schemaValidator: Validator[RenameReqSchema] =
    validator[RenameReqSchema] { req =>
      req.name.is( notEmpty )
    }
}
