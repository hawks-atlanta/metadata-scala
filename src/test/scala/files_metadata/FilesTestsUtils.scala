package org.hawksatlanta.metadata
package files_metadata

import java.security.MessageDigest
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID
import java.util.UUID

import io.restassured.response.Response
import io.restassured.RestAssured.`given`

object FilesTestsUtils {
  var wasHttpServerInitializationCalled: AtomicBoolean = new AtomicBoolean(
    false
  )

  def StartHttpServer(): Unit = {
    if (wasHttpServerInitializationCalled.compareAndSet( false, true )) {
      Main.main( Array[String]() )
    }
  }

  // -- Save files --

  // -- Save files --

  def SaveFile( payload: util.HashMap[String, Any] ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
  }

  def generateFilePayload(
      ownerUUID: UUID,
      parentDirUUID: Option[UUID]
  ): util.HashMap[String, Any] = {
    val parentUUID: String =
      if (parentDirUUID.isDefined) parentDirUUID.get.toString
      else null

    val randomUUID: UUID = UUID.randomUUID()

    val filePayload = new util.HashMap[String, Any]()
    filePayload.put( "userUUID", ownerUUID.toString )
    filePayload.put( "parentUUID", parentUUID )
    filePayload.put( "fileName", randomUUID.toString )
    filePayload.put( "fileExtension", "txt" )
    filePayload.put( "fileType", "archive" )
    filePayload.put( "fileSize", 15 )

    filePayload
  }

  def generateDirectoryPayload(
      ownerUUID: UUID,
      parentDirUUID: Option[UUID]
  ): util.HashMap[String, Any] = {
    val parentUUID: String =
      if (parentDirUUID.isDefined) parentDirUUID.get.toString
      else null

    val randomUUID: UUID = UUID.randomUUID()

    val directoryPayload = new util.HashMap[String, Any]()
    directoryPayload.put( "userUUID", ownerUUID.toString )
    directoryPayload.put( "parentUUID", parentUUID )
    directoryPayload.put( "fileName", randomUUID.toString )
    directoryPayload.put( "fileExtension", null )
    directoryPayload.put( "fileType", "directory" )
    directoryPayload.put( "fileSize", 0 )

    directoryPayload
  }

  // -- Share files --

  def ShareFile(
      ownerUUID: String,
      fileUUID: String,
      payload: util.HashMap[String, Any]
  ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ownerUUID }/${ fileUUID }"
      )
  }

  def generateShareFilePayload(
      otherUserUUID: UUID
  ): util.HashMap[String, Any] = {
    val shareFilePayload = new util.HashMap[String, Any]()
    shareFilePayload.put( "otherUserUUID", otherUserUUID.toString )
    shareFilePayload
  }

  def GetSharedWithUser( userUUID: String ): Response = {
    `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ GetShareWithUserTestsData.API_PREFIX }/${ userUUID }"
      )
  }

  def GetSharedWithWho( fileUUID: String ): Response = {
    `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ GetShareWithWhoTestsData.API_PREFIX }/${ fileUUID }"
      )
  }

  // -- Update files --

  def UpdateReadyFile(
      fileUUID: String,
      payload: util.HashMap[String, Any]
  ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .put(
        s"${ UpdateReadyFileTestsData.API_PREFIX }/${ fileUUID }"
      )
  }

  def generateReadyFilePayload(): util.HashMap[String, Any] = {
    val readyFilePayload = new util.HashMap[String, Any]()
    readyFilePayload.put( "volume", "volume_x" )
    readyFilePayload
  }

  def UpdateFileName(
      userUUID: String,
      fileUUID: String,
      payload: util.HashMap[String, Any]
  ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .put(
        s"${ RenameFileTestsData.API_PREFIX }/${ userUUID }/${ fileUUID }"
      )
  }

  def generateRenameFilePayload(): util.HashMap[String, Any] = {
    val renameFilePayload = new util.HashMap[String, Any]()
    renameFilePayload.put( "name", UUID.randomUUID().toString )
    renameFilePayload
  }

  def MoveFile(
      userUUID: String,
      fileUUID: String,
      payload: util.HashMap[String, Any]
  ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .put(
        s"${ MoveFileTestsData.API_PREFIX }/${ userUUID }/${ fileUUID }"
      )
  }

  def generateMoveFilePayload( parentUUID: UUID ): util.HashMap[String, Any] = {
    val moveFilePayload = new util.HashMap[String, Any]()
    moveFilePayload.put( "parentUUID", parentUUID.toString )
    moveFilePayload
  }

  // -- Get files metadata --

  def GetFileMetadata( fileUUID: String ): Response = {
    `given`()
      .port( 8080 )
      .when()
      .get(
        s"${ GetFileMetadataTestsData.API_PREFIX }/${ fileUUID }"
      )
  }

}
