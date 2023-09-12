package org.hawksatlanta.metadata
package fruit.infrastructure

import fruit.domain.{Fruit, Repository}
import shared.infrastructure.PostgreSQLPool

import com.zaxxer.hikari.HikariDataSource

class PostgreSQLRepository extends Repository{
  private val pool: HikariDataSource = PostgreSQLPool.getInstance()

  override def get_fruits(): List[Fruit] = {
    val connection = pool.getConnection()

    try {
      // Execute the query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT id, name, price FROM fruits")

      // Parse into domain entity
      var fruits: List[Fruit] = List()

      while (resultSet.next()) {
        fruits = fruits :+ Fruit(
          id = resultSet.getString("id"),
          name = resultSet.getString("name"),
          price = resultSet.getFloat("price")
        )
      }

      // Return the resulting list
      fruits
    } finally {
      connection.close()
    }
  }

  override def save(fruit: Fruit): Unit = {
    val connection = pool.getConnection()

    try {
      val statement = connection.prepareStatement("INSERT INTO fruits (name, price) VALUES (?, ?)")
      statement.setString(1, fruit.name)
      statement.setFloat(2, fruit.price)
      statement.executeUpdate()
    } finally {
      connection.close()
    }
  }
}
