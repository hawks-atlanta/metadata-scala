package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object GetShareWithWhoTestsData {
  val API_PREFIX: String    = "/api/v1/files/shared_with_who"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  var sharedFileUUID: UUID   = _
  var unsharedFileUUID: UUID = _
}

@OrderWith( classOf[Alphanumeric] )
class GetSharedWithWhoTests extends JUnitSuite {
  def saveAndShareFilesToCheck(): Unit = {
    // Save and share a file
    val filePayload = FilesTestsUtils.generateFilePayload(
      GetShareWithWhoTestsData.OWNER_USER_UUID,
      None
    )
    val sharePayload = FilesTestsUtils.generateShareFilePayload(
      GetShareWithWhoTestsData.OTHER_USER_UUID
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( filePayload )
    GetShareWithWhoTestsData.sharedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    FilesTestsUtils.ShareFile(
      GetShareWithWhoTestsData.OWNER_USER_UUID.toString,
      GetShareWithWhoTestsData.sharedFileUUID.toString,
      sharePayload
    )

    // Save and don't share a file
    val secondFilePayload = FilesTestsUtils.generateFilePayload(
      GetShareWithWhoTestsData.OWNER_USER_UUID,
      None
    )

    val saveSecondFileResponse = FilesTestsUtils.SaveFile( secondFilePayload )
    GetShareWithWhoTestsData.unsharedFileUUID =
      UUID.fromString( saveSecondFileResponse.jsonPath().get( "uuid" ) )
  }

  @Before
  def before(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_SharedWithWhoBadRequest(): Unit = {
    saveAndShareFilesToCheck()

    val response = FilesTestsUtils.GetSharedWithWho(
      "not_an_uuid"
    )

    assert( response.statusCode() == 400 )
  }

  @Test
  def T2_SharedWithWhoNotFound(): Unit = {
    val response = FilesTestsUtils.GetSharedWithWho(
      UUID.randomUUID().toString
    )
    assert( response.statusCode() == 404 )
  }

  @Test
  def T3_SharedWithWhoSuccess(): Unit = {
    val response = FilesTestsUtils.GetSharedWithWho(
      GetShareWithWhoTestsData.sharedFileUUID.toString
    )
    val responseJson = response.jsonPath()

    assert( response.statusCode() == 200 )
    assert( responseJson.getList( "shared_with" ).size() == 1 )
    assert(
      responseJson
        .getList( "shared_with" )
        .get( 0 )
        .equals( GetShareWithWhoTestsData.OTHER_USER_UUID.toString )
    )
  }

  @Test
  def T4_SharedWithWhoEmpty(): Unit = {
    val response = FilesTestsUtils.GetSharedWithWho(
      GetShareWithWhoTestsData.unsharedFileUUID.toString
    )
    val responseJson = response.jsonPath()

    assert( response.statusCode() == 200 )
    assert( responseJson.getList( "shared_with" ).size() == 0 )
  }
}
