package org.hawksatlanta.metadata
package files_metadata.infrastructure.requests

import com.wix.accord.dsl._
import com.wix.accord.Validator

case class MarkAsReadyReqSchema(
    volume: String
)

object MarkAsReadyReqSchema {
  import upickle.default._
  implicit def rw: ReadWriter[MarkAsReadyReqSchema] =
    macroRW[MarkAsReadyReqSchema]

  val schemaValidator: Validator[MarkAsReadyReqSchema] =
    validator[MarkAsReadyReqSchema] { req =>
      req.volume.is( notEmpty )
    }
}
