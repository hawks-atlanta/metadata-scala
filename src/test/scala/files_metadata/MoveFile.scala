package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object MoveFileTestsData {
  val API_PREFIX: String = "/api/v1/files/move"
  val USER_UUID: UUID    = UUID.randomUUID()

  var savedDirectoryUUID: UUID  = _
  var savedFileUUID: UUID       = _
  var secondSavedFileUUID: UUID = _
}

@OrderWith( classOf[Alphanumeric] )
class MoveFile extends JUnitSuite {
  def saveFileToMove(): Unit = {
    // Save a file
    val saveFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = MoveFileTestsData.USER_UUID,
      parentDirUUID = None
    )

    val saveFileResponse = FilesTestsUtils.SaveFile( saveFilePayload )
    MoveFileTestsData.savedFileUUID =
      UUID.fromString( saveFileResponse.jsonPath().get( "uuid" ) )

    // Save a directory
    val saveDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = MoveFileTestsData.USER_UUID,
      parentDirUUID = None
    )

    val saveDirectoryResponse = FilesTestsUtils.SaveFile( saveDirectoryPayload )
    MoveFileTestsData.savedDirectoryUUID =
      UUID.fromString( saveDirectoryResponse.jsonPath().get( "uuid" ) )

    // Save a second file
    val saveSecondFilePayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = MoveFileTestsData.USER_UUID,
      parentDirUUID = None
    )

    val saveSecondFileResponse =
      FilesTestsUtils.SaveFile( saveSecondFilePayload )
    MoveFileTestsData.secondSavedFileUUID =
      UUID.fromString( saveSecondFileResponse.jsonPath().get( "uuid" ) )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_MoveFileBadRequest(): Unit = {
    saveFileToMove()

    // -- Bad userUUID
    val movePayload = FilesTestsUtils.generateMoveFilePayload(
      parentUUID = MoveFileTestsData.savedDirectoryUUID
    )
    val badUserUUIDResponse = FilesTestsUtils.MoveFile(
      userUUID = "not_an_uuid",
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( badUserUUIDResponse.statusCode() == 400 )

    // -- Bad fileUUID
    val badFileUUIDResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = "not_an_uuid",
      payload = movePayload
    )
    assert( badFileUUIDResponse.statusCode() == 400 )

    // -- Bad parentUUID
    movePayload.put( "parentUUID", "not_an_uuid" )
    val badParentUUIDResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( badParentUUIDResponse.statusCode() == 400 )
  }

  @Test
  def T2_MoveFileNotFound(): Unit = {
    // File not found
    val movePayload = FilesTestsUtils.generateMoveFilePayload(
      parentUUID = MoveFileTestsData.savedDirectoryUUID
    )
    val notFoundResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = UUID.randomUUID().toString,
      payload = movePayload
    )
    assert( notFoundResponse.statusCode() == 404 )

    // Parent directory not found
    movePayload.put( "parentUUID", UUID.randomUUID().toString )
    val parentNotFoundResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( parentNotFoundResponse.statusCode() == 404 )
  }

  @Test
  def T3_MoveFileParentIsNotADirectory(): Unit = {
    val movePayload = FilesTestsUtils.generateMoveFilePayload(
      parentUUID = MoveFileTestsData.secondSavedFileUUID
    )
    val parentNotADirectoryResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( parentNotADirectoryResponse.statusCode() == 400 )
  }

  @Test
  def T4_MoveFileForbidden(): Unit = {
    val movePayload = FilesTestsUtils.generateMoveFilePayload(
      parentUUID = MoveFileTestsData.savedDirectoryUUID
    )
    val forbiddenResponse = FilesTestsUtils.MoveFile(
      userUUID = UUID.randomUUID().toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( forbiddenResponse.statusCode() == 403 )
  }

  @Test
  def T5_MoveFile(): Unit = {
    // Move the file to the saved directory
    val movePayload = FilesTestsUtils.generateMoveFilePayload(
      parentUUID = MoveFileTestsData.savedDirectoryUUID
    )
    val moveResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( moveResponse.statusCode() == 204 )

    // Try to move the file again
    val secondMoveResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( secondMoveResponse.statusCode() == 409 )

    // Move the file to root
    movePayload.put( "parentUUID", null )
    val moveFileToRootResponse = FilesTestsUtils.MoveFile(
      userUUID = MoveFileTestsData.USER_UUID.toString,
      fileUUID = MoveFileTestsData.savedFileUUID.toString,
      payload = movePayload
    )
    assert( moveFileToRootResponse.statusCode() == 204 )
  }
}
