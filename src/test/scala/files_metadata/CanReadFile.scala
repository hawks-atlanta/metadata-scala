package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import io.restassured.RestAssured.`given`
import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object CanReadFileTestsData {
  val API_PREFIX: String    = "/api/v1/files/can_read"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  var savedFirstLevelDirectoryUUID: UUID  = _
  var savedSecondLevelDirectoryUUID: UUID = _
  var savedThirdLevelDirectoryUUID: UUID  = _

  var directlySharedFileUUID: UUID   = _
  var indirectlySharedFileUUID: UUID = _
  var unsharedFileUUID: UUID         = _
}

@OrderWith( classOf[Alphanumeric] )
class CanReadFileTests extends JUnitSuite {
  def saveAndShareFilesToCheck(): Unit = {
    // Save the directories
    val thirdLvlDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val thirdLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( thirdLvlDirectoryPayload )
    CanReadFileTestsData.savedThirdLevelDirectoryUUID = UUID.fromString(
      thirdLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    val secondLvlDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID,
      parentDirUUID = Some(
        CanReadFileTestsData.savedThirdLevelDirectoryUUID
      )
    )

    val secondLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( secondLvlDirectoryPayload )
    CanReadFileTestsData.savedSecondLevelDirectoryUUID = UUID.fromString(
      secondLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    val firstLvlDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID,
      parentDirUUID = Some(
        CanReadFileTestsData.savedSecondLevelDirectoryUUID
      )
    )

    val firstLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( firstLvlDirectoryPayload )
    CanReadFileTestsData.savedFirstLevelDirectoryUUID = UUID.fromString(
      firstLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    // Save the first file
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID,
      parentDirUUID = Some(
        CanReadFileTestsData.savedFirstLevelDirectoryUUID
      )
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    CanReadFileTestsData.indirectlySharedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Save the second file
    saveFilePayload.put( "parentUUID", null )
    saveFilePayload.put( "fileName", "directly_shared.txt" )

    val saveFileResponse2 = FilesTestsUtils.SaveFile( saveFilePayload )
    CanReadFileTestsData.directlySharedFileUUID =
      UUID.fromString( saveFileResponse2.jsonPath().get( "uuid" ) )

    // Save the third file
    saveFilePayload.put( "fileName", "unshared.txt" )

    val saveFileResponse3 = FilesTestsUtils.SaveFile( saveFilePayload )
    CanReadFileTestsData.unsharedFileUUID =
      UUID.fromString( saveFileResponse3.jsonPath().get( "uuid" ) )

    // Share the third level directory
    val sharePayload = FilesTestsUtils.generateShareFilePayload(
      otherUserUUID = CanReadFileTestsData.OTHER_USER_UUID
    )

    FilesTestsUtils.ShareFile(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = CanReadFileTestsData.savedThirdLevelDirectoryUUID.toString,
      payload = sharePayload
    )

    // Share the directly shared file
    FilesTestsUtils.ShareFile(
      ownerUUID = CanReadFileTestsData.OWNER_USER_UUID.toString,
      fileUUID = CanReadFileTestsData.directlySharedFileUUID.toString,
      payload = sharePayload
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_OwnersCanReadTheirFiles(): Unit = {
    saveAndShareFilesToCheck()

    val firstFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OWNER_USER_UUID.toString }/${ CanReadFileTestsData.indirectlySharedFileUUID.toString }"
      )
    assert( firstFileResponse.statusCode() == 204 )

    val secondFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OWNER_USER_UUID.toString }/${ CanReadFileTestsData.directlySharedFileUUID.toString }"
      )
    assert( secondFileResponse.statusCode() == 204 )

    val thirdFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OWNER_USER_UUID.toString }/${ CanReadFileTestsData.unsharedFileUUID.toString }"
      )
    assert( thirdFileResponse.statusCode() == 204 )
  }

  @Test
  def T2_SharedUsersCanReadFiles(): Unit = {
    val directlySharedFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OTHER_USER_UUID.toString }/${ CanReadFileTestsData.directlySharedFileUUID.toString }"
      )
    assert( directlySharedFileResponse.statusCode() == 204 )

    val indirectlySharedFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OTHER_USER_UUID.toString }/${ CanReadFileTestsData.indirectlySharedFileUUID.toString }"
      )
    assert( indirectlySharedFileResponse.statusCode() == 204 )
  }

  @Test
  def T3_UnsharedUsersCannotReadFiles(): Unit = {
    val unsharedFileResponse = `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ CanReadFileTestsData.API_PREFIX }/${ CanReadFileTestsData.OTHER_USER_UUID.toString }/${ CanReadFileTestsData.unsharedFileUUID.toString }"
      )
    assert( unsharedFileResponse.statusCode() == 403 )
  }
}
