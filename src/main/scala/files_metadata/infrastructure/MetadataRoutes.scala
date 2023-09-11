package org.hawksatlanta.metadata
package files_metadata.infrastructure

import ujson.Obj

case class MetadataRoutes() extends cask.Routes {
  private val basePath = "/api/v1/files"

  private val controllers = new MetadataControllers()
  controllers._init()

  @cask.post( s"${ basePath }" )
  def SaveMetadataHandler(
      request: cask.Request
  ): cask.Response[Obj] = {
    controllers.SaveMetadataController( request )
  }

  @cask.post( s"${ basePath }/share/:ownerUUID/:fileUUID" )
  def ShareMetadataHandler(
      request: cask.Request,
      ownerUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    controllers.ShareFileController( request, ownerUUID, fileUUID )
  }

  @cask.get( s"${ basePath }/can_read/:userUUID/:fileUUID" )
  def CanReadMetadataHandler(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    controllers.CanReadFileController( request, userUUID, fileUUID )
  }

  @cask.put( s"${ basePath }/ready/:archiveUUID" )
  def ReadyMetadataHandler(
      request: cask.Request,
      archiveUUID: String
  ): cask.Response[Obj] = {
    controllers.MarkArchiveAsReadyController( request, archiveUUID )
  }

  initialize()
}
