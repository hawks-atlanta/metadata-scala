package org.hawksatlanta.metadata
package shared.infrastructure

import files_metadata.infrastructure.MetadataRoutes

object CaskHTTPRouter extends cask.Main {
  override def port: Int    = 8080
  override def host: String = "0.0.0.0"

  val allRoutes = Seq(
    MetadataRoutes()
  )
}
