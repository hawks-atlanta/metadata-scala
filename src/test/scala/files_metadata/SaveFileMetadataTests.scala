package org.hawksatlanta.metadata
package files_metadata

import java.util
import java.util.UUID

import io.restassured.RestAssured.`given`
import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite
import shared.infrastructure.CommonValidator

object TestData {
  val API_PREFIX: String = "/api/v1/files"
  val USER_UUID: UUID    = UUID.randomUUID()

  private var filePayload: util.HashMap[String, Any] = _
  var directoryUUID: UUID                            = _

  def getFilePayload(): util.HashMap[String, Any] = {
    if (filePayload == null) {
      filePayload = new util.HashMap[String, Any]
      filePayload.put( "parentUUID", null )
      filePayload.put(
        "hashSum",
        "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
      )
      filePayload.put( "fileType", "archive" )
      filePayload.put( "fileName", "project.txt" )
      filePayload.put( "fileSize", 150 )
    }

    filePayload.clone().asInstanceOf[util.HashMap[String, Any]]
  }

  def setDirectoryUUID( uuid: UUID ): Unit = {
    directoryUUID = uuid
  }
}

@OrderWith( classOf[Alphanumeric] )
class SaveFileMetadataTests extends JUnitSuite {
  // Setup routes and perform migrations
  @Test
  def T0_setup(): Unit = {
    Main.main( Array() )
  }

  @Test
  // POST /api/v1/files/:user_uuid Success: Save file metadata
  def T1_SaveArchiveMetadataSuccess(): Unit = {
    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( TestData.getFilePayload() )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString( "message" ) == "Metadata was saved successfully"
    )
    assert(
      CommonValidator.validateUUID(
        responseJSON.getString( "uuid" )
      )
    )
  }

  @Test
  // POST /api/v1/files/:user_uuid Success: Save directory metadata
  def T2_SaveDirectoryMetadataSuccess(): Unit = {
    // --- Test data ---
    val payload = new util.HashMap[String, Any]()
    payload.put( "parentUUID", null )
    payload.put( "hashSum", "" )
    payload.put( "fileType", "directory" )
    payload.put( "fileName", "project" )
    payload.put( "fileSize", 0 )

    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString( "message" ) == "Metadata was saved successfully"
    )

    // --- Save the directory UUID for the next test ---
    val directoryUUID = responseJSON.getString( "uuid" )
    assert( CommonValidator.validateUUID( directoryUUID ) )
    TestData.setDirectoryUUID( UUID.fromString( directoryUUID ) )
  }

  @Test
  /* POST /api/v1/files/:user_uuid Success: Save file metadata with parent
   * directory */
  def T3_SaveArchiveMetadataWithParentSuccess(): Unit = {
    // --- Test data ---
    val payload = TestData.getFilePayload()
    payload.put( "parentUUID", TestData.directoryUUID.toString )

    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString( "message" ) == "Metadata was saved successfully"
    )
    assert(
      CommonValidator.validateUUID(
        responseJSON.getString( "uuid" )
      )
    )
  }

  @Test
  // POST /api/v1/files/:user_uuid Bad Request: Bad user_uuid parameter
  def T4_SaveArchiveMetadataBadParameter(): Unit = {
    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .when()
      .post( s"${ TestData.API_PREFIX }/1" )

    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 400 )
    assert( responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString(
        "message"
      ) == "The user_uuid parameter was not valid"
    )
  }

  @Test
  // POST /api/v1/files/:user_uuid Conflict: File already exists
  def T5_SaveArchiveMetadataConflict(): Unit = {
    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( TestData.getFilePayload() )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 409 )
    assert( responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString(
        "message"
      ) == "A file with the same name already exists in the given directory"
    )
  }

  @Test
  /* POST /api/v1/files/:user_uuid Conflict: File in parent directory already
   * exists */
  def T6_SaveArchiveMetadataWithParentConflict(): Unit = {
    // --- Test data ---
    val payload = TestData.getFilePayload()
    payload.put( "parentUUID", TestData.directoryUUID.toString )

    print( s">> Parent: ${ TestData.directoryUUID.toString } <<" )

    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 409 )
    assert( responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString(
        "message"
      ) == "A file with the same name already exists in the given directory"
    )
  }

  @Test
  // POST /api/v1/files/:user_uuid Not found: Parent directory does not exist
  def T7_SaveArchiveMetadataWithParentNotFound(): Unit = {
    // --- Test data ---
    val payload = TestData.getFilePayload()
    payload.put( "parentUUID", UUID.randomUUID().toString )

    // --- Request ---
    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ TestData.API_PREFIX }/${ TestData.USER_UUID.toString }" )
    val responseJSON = response.jsonPath()

    // --- Assertions ---
    assert( response.statusCode() == 404 )
    assert( responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString(
        "message"
      ) == "The user does not own a file or directory with the given UUID"
    )
  }
}
