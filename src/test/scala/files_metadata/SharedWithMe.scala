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
    // Save a directory
    val saveDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = GetShareWithUserTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val saveDirectoryResponse = FilesTestsUtils.SaveFile( saveDirectoryPayload )
    GetShareWithUserTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )

    // Save a file
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = GetShareWithUserTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )
    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    GetShareWithUserTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Mark the file as ready
    FilesTestsUtils.UpdateReadyFile(
      GetShareWithUserTestsData.savedFileUUID.toString,
      FilesTestsUtils.generateReadyFilePayload()
    )

    // Share the file and the directory
    val shareFilePayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = GetShareWithUserTestsData.OTHER_USER_UUID
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