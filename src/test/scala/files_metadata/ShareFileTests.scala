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
      sharePayload = new java.util.HashMap[String, Any]
      sharePayload.put( "otherUserUUID", OTHER_USER_UUID.toString )
    }

    sharePayload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}

@OrderWith( classOf[Alphanumeric] )
class ShareFileTests extends JUnitSuite {
  def saveFilesToShare(): Unit = {
    // Save a file to share
    val saveFilePayload = new java.util.HashMap[String, Any]()
    saveFilePayload.put(
      "userUUID",
      ShareFileTestsData.OWNER_USER_UUID.toString
    )
    saveFilePayload.put( "parentUUID", null )
    saveFilePayload.put(
      "hashSum",
      "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
    )
    saveFilePayload.put( "fileType", "archive" )
    saveFilePayload.put( "fileName", "share.txt" )
    saveFilePayload.put( "fileSize", 150 )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    ShareFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Save a directory to share
    val saveDirectoryPayload = new java.util.HashMap[String, Any]()
    saveDirectoryPayload.put(
      "userUUID",
      ShareFileTestsData.OWNER_USER_UUID.toString
    )
    saveDirectoryPayload.put( "parentUUID", null )
    saveDirectoryPayload.put( "hashSum", "" )
    saveDirectoryPayload.put( "fileType", "directory" )
    saveDirectoryPayload.put( "fileName", "share" )
    saveDirectoryPayload.put( "fileSize", 0 )

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
  // POST /api/v1/files/share/:user_uuid/:file_uuid Bad request
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
  // POST /api/v1/files/share/:user_uuid/:file_uuid Success: Share file
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
  // POST /api/v1/files/share/:user_uuid/:file_uuid Not found
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
  // POST /api/v1/files/share/:user_uuid/:file_uuid Conflict
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
  // POST /api/v1/files/share/:user_uuid/:file_uuid Forbidden
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
