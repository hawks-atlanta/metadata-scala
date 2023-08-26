package org.hawksatlanta.metadata
package files_metadata.infrastructure

import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID

import com.zaxxer.hikari.HikariDataSource
import files_metadata.domain.ArchivesMeta
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository
import shared.infrastructure.PostgreSQLPool

class FilesMetaPostgresRepository extends FilesMetaRepository {
  private val pool: HikariDataSource = PostgreSQLPool.getInstance()

  private def saveDirectory( fileMeta: FileMeta ): Option[UUID] = {
    print( ">> Saving a directory <<" )
    val connection: Connection = pool.getConnection()

    try {
      val statemet = connection.prepareStatement(
        "INSERT INTO files (owner_uuid, parent_uuid, name) VALUES (?, ?, ?) RETURNING uuid"
      )

      statemet.setObject( 1, fileMeta.ownerUuid )
      statemet.setObject( 2, fileMeta.parentUuid.orNull )
      statemet.setString( 3, fileMeta.name )

      val result                     = statemet.executeQuery()
      var insertedUUID: Option[UUID] = None

      if (result.next()) {
        val parsedUUID = UUID.fromString( result.getString( "uuid" ) )
        insertedUUID = Some( parsedUUID )
      }

      insertedUUID
    } catch {
      case _: Exception => None
    } finally {
      connection.close()
    }
  }

  private def saveArchive(
      archivesMeta: ArchivesMeta,
      fileMeta: FileMeta
  ): Option[UUID] = {
    val connection: Connection = pool.getConnection()

    try {
      // Start a transancion
      connection.setAutoCommit( false )

      // 1. Insert the archive
      val archiveStatemet = connection.prepareStatement(
        "INSERT INTO archives (hash_sum, size, ready) VALUES (?, ?, ?) RETURNING uuid"
      )

      archiveStatemet.setString( 1, archivesMeta.hashSum )
      archiveStatemet.setLong( 2, archivesMeta.size )
      archiveStatemet.setBoolean( 3, false )

      val archiveResult             = archiveStatemet.executeQuery()
      var archiveUUID: Option[UUID] = None

      if (archiveResult.next()) {
        val parsedUUID = UUID.fromString( archiveResult.getString( "uuid" ) )
        archiveUUID = Some( parsedUUID )
      }

      if (archiveUUID.isEmpty) {
        throw DomainExceptions.ArchiveNotSavedException(
          "There was an error while saving the archive"
        )
      }

      // 2. Insert the file
      val fileStatemet = connection.prepareStatement(
        "INSERT INTO files (owner_uuid, parent_uuid, archive_uuid, name) VALUES (?, ?, ?, ?) RETURNING uuid"
      )

      fileStatemet.setObject( 1, fileMeta.ownerUuid )
      fileStatemet.setObject( 2, fileMeta.parentUuid.orNull )
      fileStatemet.setObject( 3, archiveUUID.get )
      fileStatemet.setString( 4, fileMeta.name )

      val fileResult             = fileStatemet.executeQuery()
      var fileUUID: Option[UUID] = None

      if (fileResult.next()) {
        val parsedUUID = UUID.fromString( fileResult.getString( "uuid" ) )
        fileUUID = Some( parsedUUID )
      }

      if (fileUUID.isEmpty) {
        throw DomainExceptions.FileNotSavedException(
          "There was an error while saving the file"
        )
      }

      // Commit the transaction
      connection.commit()
      fileUUID
    } catch {
      case exception: Exception =>
        connection.rollback()
        throw exception
    } finally {
      connection.close()
    }
  }

  override def saveFileMeta(
      archiveMeta: ArchivesMeta,
      fileMeta: FileMeta
  ): Unit = {
    if (archiveMeta.hashSum.isEmpty) saveDirectory( fileMeta )
    else saveArchive( archiveMeta, fileMeta )
  }

  override def getFilesMetaInRoot( ownerUuid: UUID ): Seq[FileMeta] = ???

  override def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: UUID
  ): Seq[FileMeta] = ???

  override def getFileMeta( ownerUuid: UUID, uuid: UUID ): FileMeta = ???

  override def searchFileInDirectory(
      ownerUuid: UUID,
      directoryUuid: Option[UUID],
      fileName: String
  ): Option[FileMeta] = {
    val connection: Connection = pool.getConnection()

    try {
      var statement: PreparedStatement = null

      if (directoryUuid.isEmpty) {
        statement = connection.prepareStatement(
          "SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name FROM files WHERE owner_uuid = ? AND parent_uuid IS NULL AND name = ? Limit 1"
        )

        statement.setObject( 1, ownerUuid )
        statement.setString( 2, fileName )
      } else {
        statement = connection.prepareStatement(
          "SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name FROM files WHERE owner_uuid = ? AND parent_uuid = ? AND name = ? Limit 1"
        )

        statement.setObject( 1, ownerUuid )
        statement.setObject( 2, directoryUuid.get )
        statement.setString( 3, fileName )
      }

      val result = statement.executeQuery()

      if (result.next()) {
        val parentUUIDString  = result.getString( "parent_uuid" )
        val archiveUUIDString = result.getString( "archive_uuid" )

        val parentUUID =
          if (parentUUIDString == null) None
          else Some( UUID.fromString( parentUUIDString ) )
        val archiveUUID =
          if (archiveUUIDString == null) None
          else Some( UUID.fromString( archiveUUIDString ) )

        Some(
          FileMeta(
            uuid = UUID.fromString( result.getString( "uuid" ) ),
            ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
            parentUuid = parentUUID,
            archiveUuid = archiveUUID,
            volume = result.getString( "volume" ),
            name = result.getString( "name" )
          )
        )
      } else {
        None
      }
    } catch {
      case _: Exception => None
    } finally {
      connection.close()
    }
  }

  override def updateFileStatus( uuid: UUID, ready: Boolean ): Unit = ???

  override def deleteFileMeta( ownerUuid: UUID, uuid: UUID ): Unit = ???
}
