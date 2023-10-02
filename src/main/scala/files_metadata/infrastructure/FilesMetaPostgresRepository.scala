package org.hawksatlanta.metadata
package files_metadata.infrastructure

import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID

import com.zaxxer.hikari.HikariDataSource
import files_metadata.domain.ArchiveMeta
import files_metadata.domain.DomainExceptions
import files_metadata.domain.FileExtendedMeta
import files_metadata.domain.FileMeta
import files_metadata.domain.FilesMetaRepository
import shared.infrastructure.PostgreSQLPool

class FilesMetaPostgresRepository extends FilesMetaRepository {
  private val pool: HikariDataSource = PostgreSQLPool.getInstance()

  override def saveDirectoryMeta( fileMeta: FileMeta ): UUID = {
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

  override def saveArchiveMeta(
      archivesMeta: ArchiveMeta,
      fileMeta: FileMeta
  ): UUID = {
    val connection: Connection = pool.getConnection()

    try {
      // Start a transancion
      connection.setAutoCommit( false )

      // 1. Insert the archive
      val archiveStatemet = connection.prepareStatement(
        "INSERT INTO archives (extension, size, ready) VALUES (?, ?, ?) RETURNING uuid"
      )

      archiveStatemet.setString( 1, archivesMeta.extension )
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

  override def getFilesMetaInDirectory(
      ownerUuid: UUID,
      directoryUuid: Option[UUID]
  ): Seq[FileExtendedMeta] = {
    try {
      if (directoryUuid.isEmpty) {
        getFilesInRoot( ownerUuid )
      } else {
        getFilesInParentDirectory( directoryUuid.get )
      }
    } catch {
      case exception: Exception => throw exception
    }
  }

  def getFilesInRoot(
      ownerUuid: UUID
  ): Seq[FileExtendedMeta] = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        """
          |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, extension, size, is_shared
          |FROM files_view WHERE
          |owner_uuid = ?
          |AND parent_uuid IS NULL
          |AND (
          | ready = true
          | OR archive_uuid IS NULL
          |)
          |""".stripMargin
      )
      statement.setObject( 1, ownerUuid )

      val result                           = statement.executeQuery()
      var filesMeta: Seq[FileExtendedMeta] = Seq()

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

        filesMeta = filesMeta :+ FileExtendedMeta(
          uuid = UUID.fromString( result.getString( "uuid" ) ),
          ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
          parentUuid = parentUUID,
          archiveUuid = archiveUUID,
          volume = result.getString( "volume" ),
          name = result.getString( "name" ),
          extension = result.getString( "extension" ),
          size = result.getLong( "size" ),
          isReady = true,
          isShared = result.getBoolean( "is_shared" )
        )
      }

      filesMeta
    } finally {
      connection.close()
    }
  }

  def getFilesInParentDirectory(
      directoryUuid: UUID
  ) = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        """
          |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, extension, size, is_shared
          |FROM files_view WHERE
          |parent_uuid = ?
          |AND (
          | ready = true
          | OR archive_uuid IS NULL
          | )
          |""".stripMargin
      )
      statement.setObject( 1, directoryUuid )

      val result                           = statement.executeQuery()
      var filesMeta: Seq[FileExtendedMeta] = Seq()

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

        filesMeta = filesMeta :+ FileExtendedMeta(
          uuid = UUID.fromString( result.getString( "uuid" ) ),
          ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
          parentUuid = parentUUID,
          archiveUuid = archiveUUID,
          volume = result.getString( "volume" ),
          name = result.getString( "name" ),
          extension = result.getString( "extension" ),
          size = result.getLong( "size" ),
          isReady = true,
          isShared = result.getBoolean( "is_shared" )
        )
      }

      filesMeta
    } finally {
      connection.close()
    }
  }

  override def getFileMeta( uuid: UUID ): FileMeta = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, is_shared FROM files WHERE uuid = ?"
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
          name = result.getString( "name" ),
          isShared = result.getBoolean( "is_shared" )
        )
      } else {
        throw DomainExceptions.FileNotFoundException(
          s"There is no file with the ${ uuid.toString } UUID"
        )
      }
    } finally {
      connection.close()
    }
  }

  override def getArchiveMeta( uuid: UUID ): ArchiveMeta = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "SELECT uuid, extension, size, ready FROM archives WHERE uuid = ?"
      )
      statement.setObject( 1, uuid )

      val result = statement.executeQuery()
      if (!result.next()) {
        throw DomainExceptions.FileNotFoundException(
          "There is no archive with the given UUID"
        )
      }

      ArchiveMeta(
        uuid = UUID.fromString( result.getString( "uuid" ) ),
        extension = result.getString( "extension" ),
        size = result.getLong( "size" ),
        ready = result.getBoolean( "ready" )
      )
    } finally {
      connection.close()
    }
  }

  override def getFilesSharedWithUserMeta(
      userUuid: UUID
  ): Seq[FileExtendedMeta] = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        """
          |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, extension, size, is_shared
          |FROM files_view WHERE
          |uuid IN (
          | SELECT file_uuid FROM shared_files WHERE user_uuid = ?
          |)
          |AND (
          | ready = true
          | OR archive_uuid IS NULL
          |)
          | """.stripMargin
      )
      statement.setObject( 1, userUuid )

      val result                           = statement.executeQuery()
      var filesMeta: Seq[FileExtendedMeta] = Seq()

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

        filesMeta = filesMeta :+ FileExtendedMeta(
          uuid = UUID.fromString( result.getString( "uuid" ) ),
          ownerUuid = UUID.fromString( result.getString( "owner_uuid" ) ),
          parentUuid = parentUUID,
          archiveUuid = archiveUUID,
          volume = result.getString( "volume" ),
          name = result.getString( "name" ),
          extension = result.getString( "extension" ),
          size = result.getLong( "size" ),
          isReady = true,
          isShared = result.getBoolean( "is_shared" )
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
          """
            |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, is_shared
            |FROM files WHERE owner_uuid = ?
            |AND parent_uuid IS NULL
            |AND name = ? 
            |Limit 1
            |""".stripMargin
        )

        statement.setObject( 1, ownerUuid )
        statement.setString( 2, fileName )
      } else {
        statement = connection.prepareStatement(
          """
            |SELECT uuid, owner_uuid, parent_uuid, archive_uuid, volume, name, is_shared FROM files
            |WHERE owner_uuid = ?
            |AND parent_uuid = ?
            |AND name = ?
            |Limit 1
            |""".stripMargin
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
            name = result.getString( "name" ),
            isShared = result.getBoolean( "is_shared" )
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
    } finally {
      connection.close()
    }
  }

  override def shareFile( fileUUID: UUID, userUUID: UUID ): Unit = {
    val connection: Connection = pool.getConnection()
    connection.setAutoCommit( false )

    try {
      val shareStatement = connection.prepareStatement(
        "INSERT INTO shared_files (file_uuid, user_uuid) VALUES (?, ?)"
      )
      shareStatement.setObject( 1, fileUUID )
      shareStatement.setObject( 2, userUUID )
      shareStatement.executeUpdate()

      val updateStatement = connection.prepareStatement(
        "UPDATE files SET is_shared = true WHERE uuid = ?"
      )
      updateStatement.setObject( 1, fileUUID )
      updateStatement.executeUpdate()

      connection.commit()
    } finally {
      connection.close()
    }
  }

  override def canUserReadFile( userUuid: UUID, fileUuid: UUID ): Boolean = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement( "SELECT can_read(?, ?)" )
      statement.setObject( 1, userUuid )
      statement.setObject( 2, fileUuid )

      val result = statement.executeQuery()
      if (result.next()) {
        result.getBoolean( 1 )
      } else {
        false
      }
    } finally {
      connection.close()
    }
  }

  override def updateArchiveToReady(
      file: FileMeta,
      volume: String
  ): Unit = {
    val connection: Connection = pool.getConnection()
    connection.setAutoCommit( false )

    try {
      val updateArchiveStatement = connection.prepareStatement(
        "UPDATE archives SET ready = true WHERE uuid = ?"
      )
      updateArchiveStatement.setObject( 1, file.archiveUuid.get )
      updateArchiveStatement.executeUpdate()

      val updateFileStatement = connection.prepareStatement(
        "UPDATE files SET volume = ? WHERE uuid = ?"
      )
      updateFileStatement.setString( 1, volume )
      updateFileStatement.setObject( 2, file.uuid )
      updateFileStatement.executeUpdate()

      connection.commit()
    } finally {
      connection.close()
    }
  }

  override def updateFileName(
      fileUUID: UUID,
      newName: String
  ): Unit = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "UPDATE files SET name = ? WHERE uuid = ?"
      )
      statement.setString( 1, newName )
      statement.setObject( 2, fileUUID )

      statement.executeUpdate()
    } finally {
      connection.close()
    }
  }

  override def updateFileParent(
      fileUUID: UUID,
      parentUUID: Option[UUID]
  ): Unit = {
    val connection: Connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement(
        "UPDATE files SET parent_uuid = ? WHERE uuid = ?"
      )
      statement.setObject( 1, parentUUID.orNull )
      statement.setObject( 2, fileUUID )

      statement.executeUpdate()
    } finally {
      connection.close()
    }
  }

  override def deleteFileMeta( ownerUuid: UUID, uuid: UUID ): Unit = ???
}
