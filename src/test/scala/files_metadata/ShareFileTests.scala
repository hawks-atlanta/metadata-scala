package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object ShareFileTestsData {
  val API_PREFIX: String    = "/api/v1/files/share"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  private var sharePayload: java.util.HashMap[String, Any] = _
  var savedDirectoryUUID: UUID                             = _
  var savedFileUUID: UUID                                  = _

  def getSharePayload(): java.util.HashMap[String, Any] = {
    if (sharePayload == null) {
      sharePayload = FilesTestsUtils.generateShareFilePayload(
        otherUserUUID = OTHER_USER_UUID
      )
    }

    sharePayload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}

@OrderWith( classOf[Alphanumeric] )
class ShareFileTests extends JUnitSuite {
  def saveFilesToShare(): Unit = {
    // Save a file to share
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    ShareFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Save a directory to share
    val saveDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val saveDirectoryResponse =
      FilesTestsUtils.SaveFile( saveDirectoryPayload )
    ShareFileTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_ShareFileBadRequest(): Unit = {
    saveFilesToShare()

    // 1. Bad otherUserUUID
    val requestBody = ShareFileTestsData.getSharePayload()
    requestBody.put( "otherUserUUID", "Not an UUID" )

    val response = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = ShareFileTestsData.savedFileUUID.toString,
      payload = requestBody
    )
    assert( response.statusCode() == 400 )
    assert( response.jsonPath().getBoolean( "error" ) )

    // 2. Bad ownerUserUUID
    val response2 = FilesTestsUtils.ShareFile(
      ownerUUID = "Not an UUID",
      fileUUID = ShareFileTestsData.savedFileUUID.toString,
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( response2.statusCode() == 400 )
    assert( response2.jsonPath().getBoolean( "error" ) )

    // 3. Bad fileUUID
    val response3 = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = "Not an UUID",
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( response3.statusCode() == 400 )
    assert( response3.jsonPath().getBoolean( "error" ) )
  }

  @Test
  def T2_ShareFileSuccess(): Unit = {
    // Share the file
    val fileResponse = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = ShareFileTestsData.savedFileUUID.toString,
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( fileResponse.statusCode() == 204 )

    // Share the directory
    val directoryResponse = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = ShareFileTestsData.savedDirectoryUUID.toString,
      payload = ShareFileTestsData.getSharePayload()
    )

    assert( directoryResponse.statusCode() == 204 )
  }

  @Test
  def T3_ShareFileNotFound(): Unit = {
    val response = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UUID.randomUUID().toString,
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( response.statusCode() == 404 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }

  @Test
  def T4_ShareFileConflict(): Unit = {
    // Share the same file as in "T2" again
    val response = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = ShareFileTestsData.savedFileUUID.toString,
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( response.statusCode() == 409 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }

  @Test
  def T5_ShareFileForbidden(): Unit = {
    val response = FilesTestsUtils.ShareFile(
      ownerUUID = ShareFileTestsData.OTHER_USER_UUID.toString,
      fileUUID = ShareFileTestsData.savedFileUUID.toString,
      payload = ShareFileTestsData.getSharePayload()
    )
    assert( response.statusCode() == 403 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }
}
