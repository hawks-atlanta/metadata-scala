package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import io.restassured.RestAssured.`given`
import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
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
    val thirdLvlDirectoryPayload = new java.util.HashMap[String, Any]()
    thirdLvlDirectoryPayload.put(
      "userUUID",
      CanReadFileTestsData.OWNER_USER_UUID.toString
    )
    thirdLvlDirectoryPayload.put( "parentUUID", null )
    thirdLvlDirectoryPayload.put( "hashSum", "" )
    thirdLvlDirectoryPayload.put( "fileType", "directory" )
    thirdLvlDirectoryPayload.put( "fileName", "Third Level Directory" )
    thirdLvlDirectoryPayload.put( "fileSize", 0 )

    val thirdLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( thirdLvlDirectoryPayload )

    CanReadFileTestsData.savedThirdLevelDirectoryUUID = UUID.fromString(
      thirdLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    val secondLvlDirectoryPayload = new java.util.HashMap[String, Any]()
    secondLvlDirectoryPayload.put(
      "userUUID",
      CanReadFileTestsData.OWNER_USER_UUID.toString
    )
    secondLvlDirectoryPayload.put(
      "parentUUID",
      CanReadFileTestsData.savedThirdLevelDirectoryUUID.toString
    )
    secondLvlDirectoryPayload.put( "hashSum", "" )
    secondLvlDirectoryPayload.put( "fileType", "directory" )
    secondLvlDirectoryPayload.put( "fileName", "Second Level Directory" )
    secondLvlDirectoryPayload.put( "fileSize", 0 )

    val secondLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( secondLvlDirectoryPayload )

    CanReadFileTestsData.savedSecondLevelDirectoryUUID = UUID.fromString(
      secondLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    val firstLvlDirectoryPayload = new java.util.HashMap[String, Any]()
    firstLvlDirectoryPayload.put(
      "userUUID",
      CanReadFileTestsData.OWNER_USER_UUID.toString
    )
    firstLvlDirectoryPayload.put(
      "parentUUID",
      CanReadFileTestsData.savedSecondLevelDirectoryUUID.toString
    )
    firstLvlDirectoryPayload.put( "hashSum", "" )
    firstLvlDirectoryPayload.put( "fileType", "directory" )
    firstLvlDirectoryPayload.put( "fileName", "First Level Directory" )
    firstLvlDirectoryPayload.put( "fileSize", 0 )

    val firstLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( firstLvlDirectoryPayload )

    CanReadFileTestsData.savedFirstLevelDirectoryUUID = UUID.fromString(
      firstLvlDirectoryResponse.jsonPath().get( "uuid" )
    )

    // Save the first file
    val saveFilePayload = new java.util.HashMap[String, Any]()
    saveFilePayload.put(
      "userUUID",
      CanReadFileTestsData.OWNER_USER_UUID.toString
    )
    saveFilePayload.put(
      "parentUUID",
      CanReadFileTestsData.savedFirstLevelDirectoryUUID.toString
    )
    saveFilePayload.put(
      "hashSum",
      "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
    )
    saveFilePayload.put( "fileType", "archive" )
    saveFilePayload.put( "fileName", "indirectly_shared.txt" )
    saveFilePayload.put( "fileSize", 150 )

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
    val sharePayload = new java.util.HashMap[String, Any]()
    sharePayload.put(
      "otherUserUUID",
      CanReadFileTestsData.OTHER_USER_UUID.toString
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

  @Test
  def T0_setup(): Unit = {
    // Setup routes and perform migrations
    Main.main( Array() )
  }

  @Test
  // GET /api/v1/files/can_read/:userUUID/:fileUUID Success
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
  }

  @Test
  // GET /api/v1/files/can_read/:userUUID/:fileUUID Success
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
  // GET /api/v1/files/can_read/:userUUID/:fileUUID Forbidden
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
