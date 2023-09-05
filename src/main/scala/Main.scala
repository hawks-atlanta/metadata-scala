package org.hawksatlanta.metadata

import migrations.PostgreSQLMigration
import shared.infrastructure.CaskHTTPRouter

object Main {
  def main( args: Array[String] ): Unit = {
    PostgreSQLMigration.migrate()
    CaskHTTPRouter.main( args )
  }
}
