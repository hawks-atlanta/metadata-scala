package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object UpdateReadyFileTestsData {
  val API_PREFIX: String = "/api/v1/files/ready"
  val OWNER_UUID: UUID   = UUID.randomUUID()

  var payload: java.util.HashMap[String, Any] = _
  var savedFileUUID: UUID                     = _
  var savedDirectoryUUID: UUID                = _

  def getPayloadCopy(): java.util.HashMap[String, Any] = {
    if (payload == null) {
      payload = new java.util.HashMap[String, Any]()
      payload.put( "volume", "volume_x" )
    }

    payload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}

@OrderWith( classOf[Alphanumeric] )
class UpdateReadyFile extends JUnitSuite {
  def saveFilesToBeUpdated(): Unit = {
    val filePayload = new java.util.HashMap[String, Any]()
    filePayload.put(
      "userUUID",
      UpdateReadyFileTestsData.OWNER_UUID.toString
    )
    filePayload.put( "parentUUID", null )
    filePayload.put(
      "hashSum",
      "71988c4d8e0803ba4519f0b2864c1331c14a1890bf8694e251379177bfedb5c3"
    )
    filePayload.put( "fileType", "archive" )
    filePayload.put( "fileName", "File to mark as ready.txt" )
    filePayload.put( "fileSize", 15 )

    val fileResponse = FilesTestsUtils.SaveFile( filePayload )
    UpdateReadyFileTestsData.savedFileUUID = UUID.fromString(
      fileResponse.body().jsonPath().getString( "uuid" )
    )

    val directoryPayload = new java.util.HashMap[String, Any]()
    directoryPayload.put(
      "userUUID",
      UpdateReadyFileTestsData.OWNER_UUID.toString
    )
    directoryPayload.put( "parentUUID", null )
    directoryPayload.put( "hashSum", "" )
    directoryPayload.put( "fileType", "directory" )
    directoryPayload.put( "fileName", "Directory to mark as ready" )
    directoryPayload.put( "fileSize", 0 )

    val directoryResponse = FilesTestsUtils.SaveFile( directoryPayload )
    UpdateReadyFileTestsData.savedDirectoryUUID = UUID.fromString(
      directoryResponse.body().jsonPath().getString( "uuid" )
    )
  }
  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  // PUT /api/v1/files/ready/{fileUUID} Bad Request
  def T1_UpdateReadyFileBadRequest(): Unit = {
    saveFilesToBeUpdated()

    // Bad fileUUID
    val response = FilesTestsUtils.UpdateReadyFile(
      "not_an_uuid",
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( response.statusCode() == 400 )

    // Bad payload
    val payload = UpdateReadyFileTestsData.getPayloadCopy()
    payload.put( "volume", "" )

    val response2 = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedFileUUID.toString,
      payload
    )

    assert( response2.statusCode() == 400 )
  }

  @Test
  // PUT /api/v1/files/ready/{fileUUID} Not Found
  def T2_UpdateReadyFileNotFound(): Unit = {
    val response = FilesTestsUtils.UpdateReadyFile(
      UUID.randomUUID().toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( response.statusCode() == 404 )
  }

  @Test
  // PUT /api/v1/files/ready/{fileUUID} Success
  def T3_UpdateReadyFileSuccess(): Unit = {
    // Update file
    val updateFileResponse = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedFileUUID.toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( updateFileResponse.statusCode() == 204 )

    // Update directory
    val updateDirectoryResponse = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedDirectoryUUID.toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( updateDirectoryResponse.statusCode() == 204 )
  }

  @Test
  // PUT /api/v1/files/read/{fileUUID} Conflict
  def T4_UpdateReadyFileConflict(): Unit = {
    // Try to mark the file as ready again
    val updateFileResponse = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedFileUUID.toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( updateFileResponse.statusCode() == 409 )

    // Try to mark the directory as ready again
    val updateDirectoryResponse = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedDirectoryUUID.toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( updateDirectoryResponse.statusCode() == 409 )
  }
}
