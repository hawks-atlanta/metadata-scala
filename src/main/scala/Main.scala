package org.hawksatlanta.metadata

import migrations.PostgreSQLMigration

object Main {
  def main(args: Array[String]): Unit = {
    PostgreSQLMigration.migrate()
  }
}
