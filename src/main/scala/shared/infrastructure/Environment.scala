package org.hawksatlanta.metadata
package shared.infrastructure

object Environment {
  val dbHost     = sys.env.getOrElse( "DATABASE_HOST", "localhost" )
  val dbPort     = sys.env.getOrElse( "DATABASE_PORT", "5432" ).toInt
  val dbName     = sys.env.getOrElse( "DATABASE_NAME", "metadata" )
  val dbUser     = sys.env.getOrElse( "DATABASE_USER", "postgres" )
  val dbPassword = sys.env.getOrElse( "DATABASE_PASSWORD", "postgres" )
}
