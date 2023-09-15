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

  private def saveDirectory( fileMeta: FileMeta ): UUID = {
    val connection: Connection = pool.getConnection()

    try {
      val statemet = connection.prepareStatement(
        "INSERT INTO files (owner_uuid, parent_uuid, name) VALUES (?, ?, ?) RETURNING uuid"
      )

      statemet.setObject( 1, fileMeta.ownerUuid )
      statemet.setObject( 2, fileMeta.parentUuid.orNull )
      statemet.setString( 3, fileMeta.name )

      val result             = statemet.executeQuery()
      var insertedUUID: UUID = null

      if (result.next()) {
        val parsedUUID = UUID.fromString( result.getString( "uuid" ) )
        insertedUUID = parsedUUID
      } else {
        throw DomainExceptions.FileNotSavedException(
          "There was an error while saving the directory"
        )
      }

      insertedUUID
    } catch {
      case exception: Exception => throw exception
    } finally {
      connection.close()
    }
  }

  private def saveArchive(
      archivesMeta: ArchivesMeta,
      fileMeta: FileMeta
  ): UUID = {
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

      val fileResult     = fileStatemet.executeQuery()
      var fileUUID: UUID = null

      if (fileResult.next()) {
        val parsedUUID = UUID.fromString( fileResult.getString( "uuid" ) )
        fileUUID = parsedUUID
      } else {
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
  ): UUID = {
    if (archiveMeta.hashSum.isEmpty) saveDirectory( fileMeta )
    else saveArchive( archiveMeta, fileMeta )
  }

  override def getFilesMetaInRoot( ownerUuid: UUID ): Seq[FileMeta] = ???

  override def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: UUID
  ): Seq[FileMeta] = ???

  override def getFileMeta( uuid: UUID ): FileMeta = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name FROM files WHERE uuid = ?"
      )
      statement.setObject( 1, uuid )

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

        FileMeta(
          uuid = UUID.fromString( result.getString( "uuid" ) ),
          ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
          parentUuid = parentUUID,
          archiveUuid = archiveUUID,
          volume = result.getString( "volume" ),
          name = result.getString( "name" )
        )
      } else {
        throw DomainExceptions.FileNotFoundException(
          "There is no file with the given UUID"
        )
      }
    } catch {
      case exception: Exception => throw exception
    } finally {
      connection.close()
    }
  }

  override def getArchiveMeta( uuid: UUID ): ArchivesMeta = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT uuid, hash_sum, size, ready FROM archives WHERE uuid = ?"
      )
      statement.setObject( 1, uuid )

      val result = statement.executeQuery()
      if (!result.next()) {
        throw DomainExceptions.FileNotFoundException(
          "There is no archive with the given UUID"
        )
      }

      ArchivesMeta(
        uuid = UUID.fromString( result.getString( "uuid" ) ),
        hashSum = result.getString( "hash_sum" ),
        size = result.getLong( "size" ),
        ready = result.getBoolean( "ready" )
      )
    } finally {
      connection.close()
    }
  }

  override def getFilesSharedWithUserMeta( userUuid: UUID ): Seq[FileMeta] = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        """
          |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name
          |FROM files WHERE uuid IN (
          |SELECT file_uuid FROM shared_files WHERE user_uuid = ?
          |)
          | """.stripMargin
      )
      statement.setObject( 1, userUuid )

      val result                   = statement.executeQuery()
      var filesMeta: Seq[FileMeta] = Seq()

      // Parse the rows into Domain objects
      while (result.next()) {
        val parentUUIDString  = result.getString( "parent_uuid" )
        val archiveUUIDString = result.getString( "archive_uuid" )

        val parentUUID =
          if (parentUUIDString == null) None
          else Some( UUID.fromString( parentUUIDString ) )
        val archiveUUID =
          if (archiveUUIDString == null) None
          else Some( UUID.fromString( archiveUUIDString ) )

        filesMeta = filesMeta :+ FileMeta(
          uuid = UUID.fromString( result.getString( "uuid" ) ),
          ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
          parentUuid = parentUUID,
          archiveUuid = archiveUUID,
          volume = result.getString( "volume" ),
          name = result.getString( "name" )
        )
      }

      filesMeta
    } finally {
      connection.close()
    }
  }

  override def getUsersFileWasSharedWith( fileUuid: UUID ): Seq[UUID] = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT user_uuid FROM shared_files WHERE file_uuid = ?"
      )
      statement.setObject( 1, fileUuid )

      val result               = statement.executeQuery()
      var usersUUID: Seq[UUID] = Seq()

      while (result.next()) {
        usersUUID =
          usersUUID :+ UUID.fromString( result.getString( "user_uuid" ) )
      }

      usersUUID
    } finally {
      connection.close()
    }
  }

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

  override def isFileDirectlySharedWithUser(
      fileUuid: UUID,
      userUuid: UUID
  ): Boolean = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM shared_files WHERE file_uuid = ? AND user_uuid = ?"
      )

      statement.setObject( 1, fileUuid )
      statement.setObject( 2, userUuid )

      val result = statement.executeQuery()

      if (result.next()) {
        result.getInt( 1 ) > 0
      } else {
        false
      }
    } catch {
      case _: Exception => false
    } finally {
      connection.close()
    }
  }

  override def shareFile( fileUUID: UUID, userUUID: UUID ): Unit = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "INSERT INTO shared_files (file_uuid, user_uuid) VALUES (?, ?)"
      )

      statement.setObject( 1, fileUUID )
      statement.setObject( 2, userUUID )

      statement.executeUpdate()
    } catch {
      case exception: Exception => throw exception
    } finally {
      connection.close()
    }
  }

  override def canUserReadFile( userUuid: UUID, fileUuid: UUID ): Boolean = {
    val connection: Connection = pool.getConnection()

    val statement = connection.prepareStatement( "SELECT can_read(?, ?)" )
    statement.setObject( 1, userUuid )
    statement.setObject( 2, fileUuid )

    val result = statement.executeQuery()
    if (result.next()) {
      result.getBoolean( 1 )
    } else {
      false
    }
  }

  override def updateArchiveStatus(
      archiveUUID: UUID,
      ready: Boolean
  ): Unit = {
    val connection: Connection = pool.getConnection()

    val statement = connection.prepareStatement(
      "UPDATE archives SET ready = ? WHERE uuid = ?"
    )
    statement.setBoolean( 1, ready )
    statement.setObject( 2, archiveUUID )

    statement.executeUpdate()
  }

  def updateFileVolume(
      fileUUID: UUID,
      volume: String
  ): Unit = {
    val connection: Connection = pool.getConnection()

    val statement = connection.prepareStatement(
      "UPDATE files SET volume = ? WHERE uuid = ?"
    )
    statement.setString( 1, volume )
    statement.setObject( 2, fileUUID )

    statement.executeUpdate()
  }

  override def updateFileName(
      fileUUID: UUID,
      newName: String
  ): Unit = {
    val connection: Connection = pool.getConnection()

    val statement = connection.prepareStatement(
      "UPDATE files SET name = ? WHERE uuid = ?"
    )
    statement.setString( 1, newName )
    statement.setObject( 2, fileUUID )

    statement.executeUpdate()
  }

  override def deleteFileMeta( ownerUuid: UUID, uuid: UUID ): Unit = ???
}
