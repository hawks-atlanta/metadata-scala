package org.hawksatlanta.metadata
package files_metadata.infrastructure

import ujson.Obj

case class MetadataRoutes() extends cask.Routes {
  private val basePath = "/api/v1/files"

  private val controllers = new MetadataControllers()
  controllers._init()

  @cask.post( s"${ basePath }/:userUUID" )
  def SaveMetadataHandler(
      request: cask.Request,
      userUUID: String
  ): cask.Response[Obj] = {
    controllers.SaveMetadataController( request, userUUID )
  }

  initialize()
}
