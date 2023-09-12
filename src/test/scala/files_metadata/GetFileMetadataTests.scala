package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object GetFileMetadataTestsData {
  val API_PREFIX: String  = "/api/v1/files/metadata"
  val OWNER_UUID: UUID    = UUID.randomUUID()
  val VOLUME_NAME: String = "volume_x"

  var savedFileUUID: UUID      = _
  var savedDirectoryUUID: UUID = _
}
@OrderWith( classOf[Alphanumeric] )
class GetFileMetadataTests extends JUnitSuite {
  def saveFilesToBeObtained(): Unit = {
    val filePayload = new java.util.HashMap[String, Any]()
    filePayload.put(
      "userUUID",
      GetFileMetadataTestsData.OWNER_UUID.toString
    )
    filePayload.put( "parentUUID", null )
    filePayload.put(
      "hashSum",
      "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
    )
    filePayload.put( "fileType", "archive" )
    filePayload.put( "fileName", "File to get metadata.txt" )
    filePayload.put( "fileSize", 15 )

    val fileResponse = FilesTestsUtils.SaveFile( filePayload )
    GetFileMetadataTestsData.savedFileUUID = UUID.fromString(
      fileResponse.body().jsonPath().getString( "uuid" )
    )

    val directoryPayload = new java.util.HashMap[String, Any]()
    directoryPayload.put(
      "userUUID",
      GetFileMetadataTestsData.OWNER_UUID.toString
    )
    directoryPayload.put( "parentUUID", null )
    directoryPayload.put( "hashSum", "" )
    directoryPayload.put( "fileType", "directory" )
    directoryPayload.put( "fileName", "Directory to get metadata" )
    directoryPayload.put( "fileSize", 0 )

    val directoryResponse = FilesTestsUtils.SaveFile( directoryPayload )
    GetFileMetadataTestsData.savedDirectoryUUID = UUID.fromString(
      directoryResponse.body().jsonPath().getString( "uuid" )
    )
  }

  def markFilesAsReady(): Unit = {
    val updatePayload = new java.util.HashMap[String, Any]()
    updatePayload.put( "volume", GetFileMetadataTestsData.VOLUME_NAME )

    FilesTestsUtils.UpdateReadyFile(
      GetFileMetadataTestsData.savedFileUUID.toString,
      updatePayload
    )

    FilesTestsUtils.UpdateReadyFile(
      GetFileMetadataTestsData.savedDirectoryUUID.toString,
      updatePayload
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  // GET /api/v1/files/metadata/{fileUUID} Bad request
  def fileMetadataWithInvalidUUID(): Unit = {
    val response = FilesTestsUtils.GetFileMetadata( "not_an_uuid" )
    assert( response.statusCode() == 400 )
  }

  @Test
  // GET /api/v1/files/metadata/{fileUUID} File not found
  def fileMetadataWithNonExistentUUID(): Unit = {
    val response = FilesTestsUtils.GetFileMetadata( UUID.randomUUID().toString )
    assert( response.statusCode() == 404 )
  }

  @Test
  // GET /api/v1/files/metadata/{fileUUID} Non-ready file
  def fileMetadataWithNonReadyFileUUID(): Unit = {
    saveFilesToBeObtained()
    val response = FilesTestsUtils.GetFileMetadata(
      GetFileMetadataTestsData.savedFileUUID.toString
    )
    assert( response.statusCode() == 202 )
  }

  @Test
  // GET /api/v1/files/metadata/{fileUUID} Success
  def fileMetadataWithReadyFileUUID(): Unit = {
    markFilesAsReady()

    // Get the file
    val response = FilesTestsUtils.GetFileMetadata(
      GetFileMetadataTestsData.savedFileUUID.toString
    )
    assert( response.statusCode() == 200 )
    assert(
      response
        .body()
        .jsonPath()
        .getString( "volume" ) == GetFileMetadataTestsData.VOLUME_NAME
    )

    // Get the directory
    val directoryResponse = FilesTestsUtils.GetFileMetadata(
      GetFileMetadataTestsData.savedDirectoryUUID.toString
    )

    assert( directoryResponse.statusCode() == 200 )
    assert(
      directoryResponse
        .body()
        .jsonPath()
        .getString( "volume" ) == GetFileMetadataTestsData.VOLUME_NAME
    )
    assert(
      directoryResponse
        .body()
        .jsonPath()
        .getString( "archiveUUID" ) == null
    )
  }
}
