package org.hawksatlanta.metadata
package shared.infrastructure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object PostgreSQLPool {
  // Singleton instance
  private var pool: HikariDataSource = _;

  def getInstance(): HikariDataSource = {
    if (pool == null) {
      val config: HikariConfig = new HikariConfig()
      config.setJdbcUrl(
        s"jdbc:postgresql://${ Environment.dbHost }:${ Environment.dbPort }/${ Environment.dbName }"
      )
      config.setUsername( Environment.dbUser )
      config.setPassword( Environment.dbPassword )
      config.setMaximumPoolSize( 8 )

      pool = new HikariDataSource( config )
    }

    pool
  }
}
