package org.hawksatlanta.metadata
package shared.infrastructure

import scala.util.matching.Regex

object CommonValidator {
  val uuidRegex: Regex = """[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}""".r

  def validateUUID( uuid: String ): Boolean = {
    uuidRegex.matches( uuid )
  }
}
