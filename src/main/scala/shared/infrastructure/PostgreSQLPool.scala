package org.hawksatlanta.metadata
package shared.infrastructure

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

object PostgreSQLPool {
  // Singleton instance
  private var pool: HikariDataSource = _;

  def getInstance(): HikariDataSource = {
    if (pool == null) {
      print("Creating connection pool...")

      val config: HikariConfig = new HikariConfig()
      config.setJdbcUrl(s"jdbc:postgresql://${System.getenv("DATABASE_HOST")}:${System.getenv("DATABASE_PORT")}/${System.getenv("DATABASE_NAME")}")
      config.setUsername(System.getenv("DATABASE_USER"))
      config.setPassword(System.getenv("DATABASE_PASSWORD"))
      config.setMaximumPoolSize(10)

      pool = new HikariDataSource(config)
    }

    print("Returning connection pool...")
    pool
  }
}
