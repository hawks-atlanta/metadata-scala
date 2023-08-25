package org.hawksatlanta.metadata
package shared.infrastructure

import org.hawksatlanta.metadata.files_metadata.infrastructure.MetadataRoutes

object CaskHTTPRouter extends cask.Main {
  override def port: Int = 8080

  val allRoutes = Seq(
    MetadataRoutes()
  )
}
