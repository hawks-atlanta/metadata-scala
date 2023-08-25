package org.hawksatlanta.metadata
package files_metadata.infrastructure

import ujson.Obj

case class MetadataRoutes() extends cask.Routes {
  private val basePath = "/api/v1/files"

  @cask.post( s"${ basePath }/:userUUID" )
  def SaveMetadataHandler(
      request: cask.Request,
      userUUID: String
  ): cask.Response[Obj] = {
    // Redirect to the controller
    MetadataControllers.SaveMetadataController( request, userUUID )
  }

  initialize()
}
