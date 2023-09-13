package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object GetShareWithUserTestsData {
  val API_PREFIX: String    = "/api/v1/files/shared_with_me"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  var savedFileUUID: UUID      = _
  var savedDirectoryUUID: UUID = _
}

@OrderWith( classOf[Alphanumeric] )
class GetSharedWithUser extends JUnitSuite {
  def saveAndShareFilesToObtain(): Unit = {
    // Save a file and a directory
    val saveFilePayload = new java.util.HashMap[String, Any]()
    saveFilePayload.put(
      "userUUID",
      GetShareWithUserTestsData.OWNER_USER_UUID.toString
    )
    saveFilePayload.put( "parentUUID", null )
    saveFilePayload.put( "hashSum", "" )
    saveFilePayload.put( "fileType", "directory" )
    saveFilePayload.put( "fileName", "Directory to share" )
    saveFilePayload.put( "fileSize", 0 )

    val saveDirectoryResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    GetShareWithUserTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )

    saveFilePayload.put( "fileName", "File to share" )
    saveFilePayload.put( "fileType", "archive" )
    saveFilePayload.put( "fileSize", 15 )
    saveFilePayload.put(
      "hashSum",
      "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
    )
    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    GetShareWithUserTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Share the file and directory with the other user
    val shareFilePayload = new java.util.HashMap[String, Any]()
    shareFilePayload.put(
      "otherUserUUID",
      GetShareWithUserTestsData.OTHER_USER_UUID.toString
    )

    FilesTestsUtils.ShareFile(
      GetShareWithUserTestsData.OWNER_USER_UUID.toString,
      GetShareWithUserTestsData.savedDirectoryUUID.toString,
      shareFilePayload
    )
    FilesTestsUtils.ShareFile(
      GetShareWithUserTestsData.OWNER_USER_UUID.toString,
      GetShareWithUserTestsData.savedFileUUID.toString,
      shareFilePayload
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_GetSharedWithUserBadRequest(): Unit = {
    saveAndShareFilesToObtain()

    val response = FilesTestsUtils.GetSharedWithUser(
      "not_an_uuid"
    )
    assert( response.statusCode() == 400 )
  }

  @Test
  def T2_GetSharedWithUserSuccess(): Unit = {
    val response = FilesTestsUtils.GetSharedWithUser(
      GetShareWithUserTestsData.OTHER_USER_UUID.toString
    )
    assert( response.statusCode() == 200 )
    assert(
      response.jsonPath().getList( "files" ).size() == 2
    )
  }

  @Test
  def T3_GetSharedWithUserEmpty(): Unit = {
    val response = FilesTestsUtils.GetSharedWithUser(
      UUID.randomUUID().toString
    )
    assert( response.statusCode() == 200 )
    assert(
      response.jsonPath().getList( "files" ).size() == 0
    )
  }
}
