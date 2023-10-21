package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object UnShareFileTestsData {
  val API_PREFIX: String    = "/api/v1/files/unshare"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  private var unsharePayload: java.util.HashMap[String, Any] = _
  var savedDirectoryUUID: UUID                             = _
  var savedFileUUID: UUID                                  = _
  var sharedFileUUID: UUID                                 = _
  var sharedDirectoryUUID: UUID                                 = _

  def getUnsharePayload(): java.util.HashMap[String, Any] = {
    if (unsharePayload == null) {
      unsharePayload = FilesTestsUtils.generateUnshareFilePayload(
        otherUserUUID = OTHER_USER_UUID
      )
    }

    unsharePayload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}

@OrderWith( classOf[Alphanumeric] )
class UnShareFileTests extends JUnitSuite {
  def saveFilesAndShareToUnhare(): Unit = {
    // Save a file to share
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    UnShareFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Save a directory to share
    val saveDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val saveDirectoryResponse =
      FilesTestsUtils.SaveFile( saveDirectoryPayload )
    UnShareFileTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get("uuid") )

    val ShareFilePayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = UnShareFileTestsData.OTHER_USER_UUID
    )
    val ShareFileResponse = FilesTestsUtils.ShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedFileUUID.toString,
      payload = ShareFilePayload
    )
    val ShareDirectoryPayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = UnShareFileTestsData.OTHER_USER_UUID
    )
    val ShareDirectoryResponse = FilesTestsUtils.ShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedDirectoryUUID.toString,
      payload = ShareDirectoryPayload
    )


  }



  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }
  @Test
  def T1_UnShareFileBadRequest(): Unit = {
    saveFilesAndShareToUnhare()
    // 1. Bad other user
    val requestBody = ShareFileTestsData.getSharePayload()
    requestBody.put("otherUserUUID", "Not an UUID")
    val response = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedFileUUID.toString,
      payload = requestBody
    )
    assert( response.statusCode() == 400 )
    assert( response.jsonPath().getBoolean( "error" ) )
    // 2. Bad ownerUserUUID
    val response2 = FilesTestsUtils.UnShareFile(
      ownerUUID = "Not an UUID",
      fileUUID = UnShareFileTestsData.savedFileUUID.toString,
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(response2.statusCode() == 400)
    assert(response2.jsonPath().getBoolean("error"))
    // 3. Bad fileUUID
    val response3 = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = "Not an UUID",
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(response3.statusCode() == 400)
    assert(response3.jsonPath().getBoolean("error"))
  }
  @Test
  def T2_UnshareSuccess():Unit={
    //Unshare the File
    val fileResponse = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedFileUUID.toString,
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(fileResponse.statusCode() == 204)
    //Unshare the File
    val fileResponse2 = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedDirectoryUUID.toString,
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(fileResponse2.statusCode() == 204)
  }

  @Test
  def T3_UnshareFileNotFound(): Unit = {
    val response = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = UUID.randomUUID().toString,
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(response.statusCode() == 404)
    assert(response.jsonPath().getBoolean("error"))
  }

  @Test
  def T4_ShareFileForbidden(): Unit = {
    val response = FilesTestsUtils.UnShareFile(
      ownerUUID = UnShareFileTestsData.OTHER_USER_UUID.toString,
      fileUUID = UnShareFileTestsData.savedFileUUID.toString,
      payload = UnShareFileTestsData.getUnsharePayload()
    )
    assert(response.statusCode() == 403)
    assert(response.jsonPath().getBoolean("error"))
  }
}
