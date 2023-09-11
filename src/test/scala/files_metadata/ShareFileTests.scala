package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import io.restassured.RestAssured.`given`
import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
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

    val saveFileResponse = `given`()
      .port( 8080 )
      .body( saveFilePayload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )

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

    val saveDirectoryResponse = `given`()
      .port( 8080 )
      .body( saveDirectoryPayload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )

    ShareFileTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )
  }

  @Test
  def T0_setup(): Unit = {
    // Setup routes and perform migrations
    Main.main( Array() )
  }

  @Test
  // POST /api/v1/files/share/:user_uuid/:file_uuid Bad request
  def T1_ShareFileBadRequest(): Unit = {
    // 1. Bad otherUserUUID
    val requestBody = ShareFileTestsData.getSharePayload()
    requestBody.put( "otherUserUUID", "Not an UUID" )

    val response = `given`()
      .port( 8080 )
      .body( requestBody )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/${ ShareFileTestsData.savedFileUUID }"
      )

    assert( response.statusCode() == 400 )
    assert( response.jsonPath().getBoolean( "error" ) )

    // 2. Bad ownerUserUUID
    val response2 = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/NotAnUUID/${ ShareFileTestsData.savedFileUUID }"
      )

    assert( response2.statusCode() == 400 )
    assert( response2.jsonPath().getBoolean( "error" ) )

    // 3. Bad fileUUID
    val response3 = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/NotAnUUID"
      )

    assert( response3.statusCode() == 400 )
    assert( response3.jsonPath().getBoolean( "error" ) )
  }

  @Test
  // POST /api/v1/files/share/:user_uuid/:file_uuid Success: Share file
  def T2_ShareFileSuccess(): Unit = {
    saveFilesToShare()

    // Share the file
    val response = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/${ ShareFileTestsData.savedFileUUID }"
      )

    assert( response.statusCode() == 204 )

    // Share the directory
    val directoryResponse = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/${ ShareFileTestsData.savedDirectoryUUID }"
      )

    assert( directoryResponse.statusCode() == 204 )
  }

  @Test
  // POST /api/v1/files/share/:user_uuid/:file_uuid Not found
  def T3_ShareFileNotFound(): Unit = {
    val response = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/${ UUID.randomUUID() }"
      )

    assert( response.statusCode() == 404 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }

  @Test
  // POST /api/v1/files/share/:user_uuid/:file_uuid Conflict
  def T4_ShareFileConflict(): Unit = {
    // Share the same file as in "T2" again
    val response = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OWNER_USER_UUID }/${ ShareFileTestsData.savedFileUUID }"
      )

    assert( response.statusCode() == 409 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }

  @Test
  // POST /api/v1/files/share/:user_uuid/:file_uuid Forbidden
  def T5_ShareFileForbidden(): Unit = {
    val response = `given`()
      .port( 8080 )
      .body( ShareFileTestsData.getSharePayload() )
      .contentType( "application/json" )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ShareFileTestsData.OTHER_USER_UUID }/${ ShareFileTestsData.savedFileUUID }"
      )

    assert( response.statusCode() == 403 )
    assert( response.jsonPath().getBoolean( "error" ) )
  }
}
