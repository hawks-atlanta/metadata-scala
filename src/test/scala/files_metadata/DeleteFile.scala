package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object DeleteFileTestsData {
  val API_PREFIX: String    = "/api/v1/files/delete"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  private var deletePayload: java.util.HashMap[String, Any] = _
  var savedDirectoryUUID: UUID                              = _
  var savedFileUUID: UUID                                   = _
  var sharedFileUUID: UUID                                  = _
  var sharedDirectoryUUID: UUID                             = _

  def getDeletePayload(): java.util.HashMap[String, Any] = {
    if (deletePayload == null) {
      deletePayload = FilesTestsUtils.generateDeleteFilePayload()
    }

    deletePayload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}
@OrderWith( classOf[Alphanumeric] )
class deleteFileTests extends JUnitSuite {
  def saveFilesAndShareToDelete(): Unit = {
    // Save a file to share
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )
    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    DeleteFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )
    // Save a directory to share
    val saveDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )
    val saveDirectoryResponse =
      FilesTestsUtils.SaveFile( saveDirectoryPayload )
    DeleteFileTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )

    val ShareFilePayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = DeleteFileTestsData.OTHER_USER_UUID
    )
    val ShareFileResponse = FilesTestsUtils.ShareFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = DeleteFileTestsData.savedFileUUID.toString,
      payload = ShareFilePayload
    )
    val ShareDirectoryPayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = DeleteFileTestsData.OTHER_USER_UUID
    )
    val ShareDirectoryResponse = FilesTestsUtils.ShareFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = DeleteFileTestsData.savedDirectoryUUID.toString,
      payload = ShareDirectoryPayload
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }
  @Test
  def T1_DeleteFileBadRequest(): Unit = {
    saveFilesAndShareToDelete()
    // 1. Bad ownerUserUUID
    val response2 = FilesTestsUtils.DeleteFile(
      ownerUUID = "Not an UUID",
      fileUUID = DeleteFileTestsData.savedFileUUID.toString,
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( response2.statusCode() == 400 )
    assert( response2.jsonPath().getBoolean( "error" ) )
    // 2. Bad fileUUID
    val response3 = FilesTestsUtils.DeleteFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = "Not an UUID",
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( response3.statusCode() == 400 )
    assert( response3.jsonPath().getBoolean( "error" ) )
  }

  @Test
  def T2_DeleteFileNotOwner(): Unit = {
    // Delete the file not owner
    val fileResponse = FilesTestsUtils.DeleteFile(
      ownerUUID = DeleteFileTestsData.OTHER_USER_UUID.toString,
      fileUUID = DeleteFileTestsData.savedFileUUID.toString,
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( fileResponse.statusCode() == 403 )

  }
  @Test
  def T3_DeleteFileNotFoundFile(): Unit = {
    // Delete the file not owner
    val fileResponse = FilesTestsUtils.DeleteFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UUID.randomUUID().toString,
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( fileResponse.statusCode() == 404 )
  }

  @Test
  def T4_DeleteFileSuccess(): Unit = {
    val fileResponse = FilesTestsUtils.DeleteFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = DeleteFileTestsData.savedFileUUID.toString,
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( fileResponse.statusCode() == 204 )

    // Delete the directory
    val directoryResponse = FilesTestsUtils.DeleteFile(
      ownerUUID = DeleteFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = DeleteFileTestsData.savedDirectoryUUID.toString,
      payload = DeleteFileTestsData.getDeletePayload()
    )
    assert( directoryResponse.statusCode() == 204 )
  }
}
