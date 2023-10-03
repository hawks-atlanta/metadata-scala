package org.hawksatlanta.metadata
package files_metadata

import java.util.UUID

import org.junit.runner.manipulation.Alphanumeric
import org.junit.runner.OrderWith
import org.junit.Before
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

object ListFilesInDirectoresTestsData {
  val API_PREFIX: String    = "/api/v1/files/list"
  val OWNER_USER_UUID: UUID = UUID.randomUUID()
  val OTHER_USER_UUID: UUID = UUID.randomUUID()

  var savedFirstLevelDirectoryUUID: UUID  = _
  var savedSecondLevelDirectoryUUID: UUID = _

  var fileSavedInRootDirectoryUUID: UUID       = _
  var fileSavedInFirstLevelDirectoryUUID: UUID = _
}

@OrderWith( classOf[Alphanumeric] )
class ListFilesInDirectories extends JUnitSuite {
  def SaveAndShareFilesToList(): Unit = {
    // 1. Save the directories
    val firstLvlDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val firstLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( firstLvlDirectoryPayload )
    ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID =
      UUID.fromString(
        firstLvlDirectoryResponse.jsonPath().get( "uuid" )
      )

    val secondLvlDirectoryPayload = FilesTestsUtils.generateDirectoryPayload(
      ownerUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID,
      parentDirUUID = Some(
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID
      )
    )

    val secondLvlDirectoryResponse =
      FilesTestsUtils.SaveFile( secondLvlDirectoryPayload )
    ListFilesInDirectoresTestsData.savedSecondLevelDirectoryUUID =
      UUID.fromString(
        secondLvlDirectoryResponse.jsonPath().get( "uuid" )
      )

    // 2. Save the files
    val fileSavedInRootDirectoryPayload = FilesTestsUtils.generateFilePayload(
      ownerUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID,
      parentDirUUID = None
    )

    val fileSavedInRootDirectoryResponse =
      FilesTestsUtils.SaveFile( fileSavedInRootDirectoryPayload )
    ListFilesInDirectoresTestsData.fileSavedInRootDirectoryUUID =
      UUID.fromString(
        fileSavedInRootDirectoryResponse.jsonPath().get( "uuid" )
      )

    val fileSavedInFirstLevelDirectoryPayload =
      FilesTestsUtils.generateFilePayload(
        ownerUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID,
        parentDirUUID = Some(
          ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID
        )
      )

    val fileSavedInFirstLevelDirectoryResponse =
      FilesTestsUtils.SaveFile( fileSavedInFirstLevelDirectoryPayload )
    ListFilesInDirectoresTestsData.fileSavedInFirstLevelDirectoryUUID =
      UUID.fromString(
        fileSavedInFirstLevelDirectoryResponse.jsonPath().get( "uuid" )
      )

    // 3. Share the first level directory with the other user
    val shareFirstLevelDirectoryPayload =
      FilesTestsUtils.generateShareFilePayload(
        otherUserUUID = ListFilesInDirectoresTestsData.OTHER_USER_UUID
      )
    FilesTestsUtils.ShareFile(
      ownerUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      fileUUID =
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID.toString,
      payload = shareFirstLevelDirectoryPayload
    )
  }

  @Before
  def startHttpServer(): Unit = {
    FilesTestsUtils.StartHttpServer()
  }

  @Test
  def T1_ParametersAreValidated(): Unit = {
    // 1. userUUID is validated
    val response = FilesTestsUtils.ListFilesInRootDirectory(
      userUUID = "not_an_uuid"
    )

    assert( response.statusCode() == 400 )

    // 2. parentUUID is validated
    val response2 = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      directoryUUID = "not_an_uuid"
    )

    assert( response2.statusCode() == 400 )
  }

  @Test
  def T2_CanListFilesInRootDirectory(): Unit = {
    SaveAndShareFilesToList()

    /* 1. Should be able to see the directory since the file was not marked as
     * ready yet */
    val response = FilesTestsUtils.ListFilesInRootDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString
    )

    assert( response.statusCode() == 200 )
    assert(
      response.jsonPath().getList( "files" ).size() == 1
    )

    // Mark the file as ready
    FilesTestsUtils.UpdateReadyFile(
      fileUUID =
        ListFilesInDirectoresTestsData.fileSavedInRootDirectoryUUID.toString,
      payload = FilesTestsUtils.generateReadyFilePayload()
    )

    // 2. Should be able to see the file and the directory
    val response2 = FilesTestsUtils.ListFilesInRootDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString
    )

    assert( response2.statusCode() == 200 )
    assert(
      response2.jsonPath().getList( "files" ).size() == 2
    )
  }

  @Test
  def T3_CanListFilesInDirectory(): Unit = {
    /* 1. Should be able to see the nested directory since the nested file was
     * not marked as ready yet */
    val response = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      directoryUUID =
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID.toString
    )

    assert( response.statusCode() == 200 )
    assert(
      response.jsonPath().getList( "files" ).size() == 1
    )

    // Mark the file as ready
    FilesTestsUtils.UpdateReadyFile(
      fileUUID =
        ListFilesInDirectoresTestsData.fileSavedInFirstLevelDirectoryUUID.toString,
      payload = FilesTestsUtils.generateReadyFilePayload()
    )

    // 2. Should be able to see the file and the directory
    val response2 = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      directoryUUID =
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID.toString
    )

    assert( response2.statusCode() == 200 )
    assert(
      response2.jsonPath().getList( "files" ).size() == 2
    )
  }

  @Test
  def T4_UsersWithAccessToDirectoryCanListFiles(): Unit = {
    val response = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OTHER_USER_UUID.toString,
      directoryUUID =
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID.toString
    )

    assert( response.statusCode() == 200 )
    assert(
      response.jsonPath().getList( "files" ).size() == 2
    )
  }

  @Test
  def T5_UsersWithoutAccessToDirectoryCannotListFiles(): Unit = {
    val response = FilesTestsUtils.ListFilesInDirectory(
      userUUID = UUID.randomUUID().toString,
      directoryUUID =
        ListFilesInDirectoresTestsData.savedFirstLevelDirectoryUUID.toString
    )

    assert( response.statusCode() == 403 )
  }

  @Test
  def T6_ParentDirectoryMustExist(): Unit = {
    // 1. Given a non-existing parent directory, should return 404
    val response = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      directoryUUID = UUID.randomUUID().toString
    )

    assert( response.statusCode() == 404 )

    // 2. Given a file as parent directory, should return 400
    val response2 = FilesTestsUtils.ListFilesInDirectory(
      userUUID = ListFilesInDirectoresTestsData.OWNER_USER_UUID.toString,
      directoryUUID =
        ListFilesInDirectoresTestsData.fileSavedInRootDirectoryUUID.toString
    )

    assert( response2.statusCode() == 400 )
  }
}
