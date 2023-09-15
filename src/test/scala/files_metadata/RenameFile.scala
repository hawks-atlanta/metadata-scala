package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object RenameFileTestsData {
  val API_PREFIX: String = "/api/v1/files/rename"
  val USER_UUID: UUID    = UUID.randomUUID()

  var savedFileUUID: UUID = _
  var updatedName: String = _
}

@OrderWith( classOf[Alphanumeric] )
class RenameFileTests extends JUnitSuite {
  def saveFileToRename(): Unit = {
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = RenameFileTestsData.USER_UUID,
      parentDirUUID = None
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    RenameFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    FilesTestsUtils.UpdateReadyFile(
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = FilesTestsUtils.generateReadyFilePayload()
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_RenameFileBadRequest(): Unit = {
    saveFileToRename()

    // -- Bad userUUID
    val renamePayload = FilesTestsUtils.generateRenameFilePayload()
    val badUserUUIDResponse = FilesTestsUtils.UpdateFileName(
      userUUID = "not_an_uuid",
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = renamePayload
    )
    assert( badUserUUIDResponse.statusCode() == 400 )

    // -- Bad fileUUID
    val badFileUUIDResponse = FilesTestsUtils.UpdateFileName(
      userUUID = RenameFileTestsData.USER_UUID.toString,
      fileUUID = "not_an_uuid",
      payload = renamePayload
    )
    assert( badFileUUIDResponse.statusCode() == 400 )

    // -- Bad payload
    renamePayload.put( "name", "" )
    val badPayloadResponse = FilesTestsUtils.UpdateFileName(
      userUUID = RenameFileTestsData.USER_UUID.toString,
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = renamePayload
    )
    assert( badPayloadResponse.statusCode() == 400 )
  }

  @Test
  def T2_RenameFile(): Unit = {
    val renamePayload = FilesTestsUtils.generateRenameFilePayload()
    val renameResponse = FilesTestsUtils.UpdateFileName(
      userUUID = RenameFileTestsData.USER_UUID.toString,
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = renamePayload
    )
    assert( renameResponse.statusCode() == 204 )
    RenameFileTestsData.updatedName = renamePayload.get( "name" ).toString

    val getMetadataResponse = FilesTestsUtils.GetFileMetadata(
      fileUUID = RenameFileTestsData.savedFileUUID.toString
    )
    val getMetadataJson = getMetadataResponse.jsonPath()
    assert(
      getMetadataJson.getString( "name" ) == RenameFileTestsData.updatedName
    )
  }

  @Test
  def T3_RenameFileNotFound(): Unit = {
    val renamePayload = FilesTestsUtils.generateRenameFilePayload()
    val renameResponse = FilesTestsUtils.UpdateFileName(
      userUUID = RenameFileTestsData.USER_UUID.toString,
      fileUUID = UUID.randomUUID().toString,
      payload = renamePayload
    )
    assert( renameResponse.statusCode() == 404 )
  }

  @Test
  def T4_RenameFileConflict(): Unit = {
    // Save another file
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = RenameFileTestsData.USER_UUID,
      parentDirUUID = None
    )
    FilesTestsUtils.SaveFile( saveFilePayload )

    // Try to rename the file with the same name
    val renamePayload = FilesTestsUtils.generateRenameFilePayload()
    renamePayload.put( "name", saveFilePayload.get( "fileName" ).toString )

    val renameResponse = FilesTestsUtils.UpdateFileName(
      userUUID = RenameFileTestsData.USER_UUID.toString,
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = renamePayload
    )
    assert( renameResponse.statusCode() == 409 )
  }

  @Test
  def T5_RenameFileForbidden(): Unit = {
    val renamePayload = FilesTestsUtils.generateRenameFilePayload()
    val renameResponse = FilesTestsUtils.UpdateFileName(
      userUUID = UUID.randomUUID().toString,
      fileUUID = RenameFileTestsData.savedFileUUID.toString,
      payload = renamePayload
    )
    assert( renameResponse.statusCode() == 403 )
  }
}
