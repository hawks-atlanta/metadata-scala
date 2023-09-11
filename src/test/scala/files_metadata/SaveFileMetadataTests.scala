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

object SaveFileTestsData {
  val API_PREFIX: String = "/api/v1/files"
  val USER_UUID: UUID    = UUID.randomUUID()

  private var filePayload: util.HashMap[String, Any] = _
  var savedDirectoryUUID: UUID                       = _

  def getFilePayloadCopy(): util.HashMap[String, Any] = {
    if (filePayload == null) {
      filePayload = new util.HashMap[String, Any]
      filePayload.put( "userUUID", USER_UUID.toString )
      filePayload.put( "parentUUID", null )
      filePayload.put(
        "hashSum",
        "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
      )
      filePayload.put( "fileType", "archive" )
      filePayload.put( "fileName", "save.txt" )
      filePayload.put( "fileSize", 150 )
    }

    filePayload.clone().asInstanceOf[util.HashMap[String, Any]]
  }

  def setDirectoryUUID( uuid: UUID ): Unit = {
    savedDirectoryUUID = uuid
  }
}

@OrderWith( classOf[Alphanumeric] )
class SaveFileMetadataTests extends JUnitSuite {
  @Test
  // POST /api/v1/files/:user_uuid Success: Save file metadata
  def T1_SaveArchiveMetadataSuccess(): Unit = {
    val response = `given`()
      .port( 8080 )
      .body( SaveFileTestsData.getFilePayloadCopy() )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

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
    val payload = new util.HashMap[String, Any]()
    payload.put( "userUUID", SaveFileTestsData.USER_UUID.toString )
    payload.put( "parentUUID", null )
    payload.put( "hashSum", "" )
    payload.put( "fileType", "directory" )
    payload.put( "fileName", "project" )
    payload.put( "fileSize", 0 )

    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString( "message" ) == "Metadata was saved successfully"
    )

    val directoryUUID = responseJSON.getString( "uuid" )
    assert( CommonValidator.validateUUID( directoryUUID ) )
    SaveFileTestsData.setDirectoryUUID( UUID.fromString( directoryUUID ) )
  }

  @Test
  /* POST /api/v1/files/:user_uuid Success: Save file metadata with parent
   * directory */
  def T3_SaveArchiveMetadataWithParentSuccess(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", SaveFileTestsData.savedDirectoryUUID.toString )

    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

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
  // POST /api/v1/files/:user_uuid Conflict: File already exists
  def T4_SaveArchiveMetadataConflict(): Unit = {
    val response = `given`()
      .port( 8080 )
      .body( SaveFileTestsData.getFilePayloadCopy() )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

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
  def T5_SaveArchiveMetadataWithParentConflict(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", SaveFileTestsData.savedDirectoryUUID.toString )

    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

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
  def T6_SaveArchiveMetadataWithParentNotFound(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", UUID.randomUUID().toString )

    val response = `given`()
      .port( 8080 )
      .body( payload )
      .contentType( "application/json" )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 404 )
    assert( responseJSON.getBoolean( "error" ) )
    assert(
      responseJSON.getString(
        "message"
      ) == "The user does not own a file or directory with the given UUID"
    )
  }
}
