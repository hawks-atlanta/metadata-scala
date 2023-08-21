package org.hawksatlanta.metadata
package migrations

import org.flywaydb.core.Flyway

object PostgreSQLMigration {
  def migrate() = {
    val flyway = Flyway.configure()
      .dataSource(
        s"jdbc:postgresql://${System.getenv("DATABASE_HOST")}:${System.getenv("DATABASE_PORT")}/${System.getenv("DATABASE_NAME")}",
        System.getenv("DATABASE_USER"),
        System.getenv("DATABASE_PASSWORD")
      )
      .locations("filesystem:db/migrations")
      .load()

    try {
      val migrationResult = flyway.migrate()
      print(s"${migrationResult.migrationsExecuted} migrations were successfully executed.")
    }catch {
      case e: Exception => {
        print(s"Migration failed: ${e.getMessage}")
      }
    }
  }
}