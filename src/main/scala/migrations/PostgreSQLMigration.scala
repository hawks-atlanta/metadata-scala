package org.hawksatlanta.metadata
package migrations

import org.flywaydb.core.Flyway
import org.hawksatlanta.metadata.shared.infrastructure.Environment

object PostgreSQLMigration {
  def migrate(): Boolean = {
    val flyway = Flyway
      .configure()
      .dataSource(
        s"jdbc:postgresql://${ Environment.dbHost }:${ Environment.dbPort }/${ Environment.dbName }",
        Environment.dbUser,
        Environment.dbPassword
      )
      .locations( "classpath:/migrations" )
      .load()

    try {
      val migrationResult = flyway.migrate()
      print(
        s"${ migrationResult.migrationsExecuted } migrations were successfully executed."
      )
      migrationResult.success
    } catch {
      case e: Exception =>
        {
          print( s"Migration failed: ${ e.getMessage }" )
        }
        false
    }
  }
}
