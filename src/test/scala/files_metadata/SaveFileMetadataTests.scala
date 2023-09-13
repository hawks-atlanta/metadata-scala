package org.hawksatlanta.metadata
package files_metadata

import java.util
import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
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
      filePayload = FilesTestsUtils.generateFilePayload(
        ownerUUID = USER_UUID,
        parentDirUUID = None
      )
    }

    filePayload.clone().asInstanceOf[util.HashMap[String, Any]]
  }

  def setDirectoryUUID( uuid: UUID ): Unit = {
    savedDirectoryUUID = uuid
  }
}

@OrderWith( classOf[Alphanumeric] )
class SaveFileMetadataTests extends JUnitSuite {
  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_SaveArchiveMetadataSuccess(): Unit = {
    val response = FilesTestsUtils.SaveFile(
      SaveFileTestsData.getFilePayloadCopy()
    )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      CommonValidator.validateUUID(
        responseJSON.getString( "uuid" )
      )
    )
  }

  @Test
  def T2_SaveDirectoryMetadataSuccess(): Unit = {
    val payload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = SaveFileTestsData.USER_UUID,
      parentDirUUID = None
    )

    val response      = FilesTestsUtils.SaveFile( payload )
    val responseJSON  = response.jsonPath()
    val directoryUUID = responseJSON.getString( "uuid" )

    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert( CommonValidator.validateUUID( directoryUUID ) )

    SaveFileTestsData.setDirectoryUUID( UUID.fromString( directoryUUID ) )
  }

  @Test
  def T3_SaveArchiveMetadataWithParentSuccess(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", SaveFileTestsData.savedDirectoryUUID.toString )

    val response     = FilesTestsUtils.SaveFile( payload )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 201 )
    assert( !responseJSON.getBoolean( "error" ) )
    assert(
      CommonValidator.validateUUID(
        responseJSON.getString( "uuid" )
      )
    )
  }

  @Test
  def T4_SaveArchiveMetadataConflict(): Unit = {
    val response = FilesTestsUtils.SaveFile(
      SaveFileTestsData.getFilePayloadCopy()
    )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 409 )
    assert( responseJSON.getBoolean( "error" ) )
  }

  @Test
  def T5_SaveArchiveMetadataWithParentConflict(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", SaveFileTestsData.savedDirectoryUUID.toString )

    val response     = FilesTestsUtils.SaveFile( payload )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 409 )
    assert( responseJSON.getBoolean( "error" ) )
  }

  @Test
  def T6_SaveArchiveMetadataWithParentNotFound(): Unit = {
    val payload = SaveFileTestsData.getFilePayloadCopy()
    payload.put( "parentUUID", UUID.randomUUID().toString )

    val response     = FilesTestsUtils.SaveFile( payload )
    val responseJSON = response.jsonPath()

    assert( response.statusCode() == 404 )
    assert( responseJSON.getBoolean( "error" ) )
  }
}
