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
    if (payload == null) payload = FilesTestsUtils.generateReadyFilePayload()
    payload.clone().asInstanceOf[java.util.HashMap[String, Any]]
  }
}

@OrderWith( classOf[Alphanumeric] )
class UpdateReadyFile extends JUnitSuite {
  def saveFilesToBeUpdated(): Unit = {
    val filePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = UpdateReadyFileTestsData.OWNER_UUID,
      parentDirUUID = None
    )

    val fileResponse = FilesTestsUtils.SaveFile( filePayload )
    UpdateReadyFileTestsData.savedFileUUID = UUID.fromString(
      fileResponse.body().jsonPath().getString( "uuid" )
    )

    val directoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = UpdateReadyFileTestsData.OWNER_UUID,
      parentDirUUID = None
    )

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
  def T2_UpdateReadyFileNotFound(): Unit = {
    val response = FilesTestsUtils.UpdateReadyFile(
      UUID.randomUUID().toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( response.statusCode() == 404 )
  }

  @Test
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

    assert( updateDirectoryResponse.statusCode() == 409 )
  }

  @Test
  def T4_UpdateReadyFileConflict(): Unit = {
    // Try to mark the file as ready again
    val updateFileResponse = FilesTestsUtils.UpdateReadyFile(
      UpdateReadyFileTestsData.savedFileUUID.toString,
      UpdateReadyFileTestsData.getPayloadCopy()
    )

    assert( updateFileResponse.statusCode() == 409 )
  }
}
