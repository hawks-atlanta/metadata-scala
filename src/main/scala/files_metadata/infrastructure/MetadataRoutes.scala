package org.hawksatlanta.metadata
package files_metadata.infrastructure

import shared.infrastructure.StdoutLogger
import ujson.Obj

case class MetadataRoutes() extends cask.Routes {
  private val basePath = "/api/v1/files"

  private val controllers = new MetadataControllers()
  controllers._init()

  private val listMetadataEndpoint = s"$basePath/list/:userUUID"
  @cask.get( listMetadataEndpoint )
  def ListMetadataHandler(
      userUUID: String,
      parentUUID: Option[String] = None
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      listMetadataEndpoint,
      controllers.ListFilesController( userUUID, parentUUID )
    )
  }

  private val saveMetadataEndpoint = s"$basePath"
  @cask.post( saveMetadataEndpoint )
  def SaveMetadataHandler(
      request: cask.Request
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      saveMetadataEndpoint,
      controllers.SaveMetadataController( request )
    )
  }

  private val shareMetadataEndpoint =
    s"$basePath/share/:ownerUUID/:fileUUID"
  @cask.post( shareMetadataEndpoint )
  def ShareMetadataHandler(
      request: cask.Request,
      ownerUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      shareMetadataEndpoint,
      controllers.ShareFileController( request, ownerUUID, fileUUID )
    )
  }

  private val canReadMetadataEndpoint =
    s"$basePath/can_read/:userUUID/:fileUUID"
  @cask.get( canReadMetadataEndpoint )
  def CanReadMetadataHandler(
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      canReadMetadataEndpoint,
      controllers.CanReadFileController( userUUID, fileUUID )
    )
  }

  private val markFileAsReadyEndpoint = s"$basePath/ready/:fileUUID"
  @cask.put( markFileAsReadyEndpoint )
  def ReadyMetadataHandler(
      request: cask.Request,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      markFileAsReadyEndpoint,
      controllers.MarkFileAsReadyController( request, fileUUID )
    )
  }

  private val getMetadataEndpoint = s"$basePath/metadata/:fileUUID"
  @cask.get( getMetadataEndpoint )
  def GetFileMetadataHandler(
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      getMetadataEndpoint,
      controllers.GetFileMetadataController( fileUUID )
    )
  }

  private val getFilesSharedWithUserEndpoint =
    s"$basePath/shared_with_me/:userUUID"
  @cask.get( getFilesSharedWithUserEndpoint )
  def GetSharedWithMeHandler(
      userUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      getFilesSharedWithUserEndpoint,
      controllers.GetSharedWithMeController( userUUID )
    )
  }

  private val getSharedWithWhoEndpoint = s"$basePath/shared_with_who/:fileUUID"
  @cask.get( getSharedWithWhoEndpoint )
  def GetSharedWithWhoHandler(
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      getSharedWithWhoEndpoint,
      controllers.GetSharedWithWhoController( fileUUID )
    )
  }

  private val renameFileEndpoint = s"$basePath/rename/:userUUID/:fileUUID"
  @cask.put( renameFileEndpoint )
  def RenameFileHandler(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      renameFileEndpoint,
      controllers.RenameFileController( request, userUUID, fileUUID )
    )
  }

  private val moveFileEndpoint = s"$basePath/move/:userUUID/:fileUUID"
  @cask.put( moveFileEndpoint )
  def MoveFileHandler(
      request: cask.Request,
      userUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      moveFileEndpoint,
      controllers.MoveFileController( request, userUUID, fileUUID )
    )
  }

  private val unShareMetadataEndpoint =
    s"$basePath/unshare/:ownerUUID/:fileUUID"

  @cask.post( unShareMetadataEndpoint )
  def UnShareMetadataHandler(
      request: cask.Request,
      ownerUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      unShareMetadataEndpoint,
      controllers.UnShareFileController( request, ownerUUID, fileUUID )
    )
  }
  private val deleteMetadataEndpoint =
    s"$basePath/:ownerUUID/:fileUUID"
  @cask.delete( deleteMetadataEndpoint )
  def deleteMetadataHandler(
      request: cask.Request,
      ownerUUID: String,
      fileUUID: String
  ): cask.Response[Obj] = {
    StdoutLogger.logAndReturnEndpointResponse(
      deleteMetadataEndpoint,
      controllers.DeleteFileController( request, ownerUUID, fileUUID )
    )
  }

  initialize()
}
