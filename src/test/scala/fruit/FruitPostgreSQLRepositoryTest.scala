package org.hawksatlanta.metadata
package fruit

import migrations.PostgreSQLMigration
import fruit.domain.Fruit
import fruit.application.UseCases
import fruit.infrastructure.PostgreSQLRepository

import org.junit.Test
import org.junit.runner.OrderWith
import org.junit.runner.manipulation.Alphanumeric
import org.scalatestplus.junit.JUnitSuite

@OrderWith(classOf[Alphanumeric])
class FruitPostgreSQLRepositoryTest extends JUnitSuite {
  val fruit_repository = new PostgreSQLRepository()
  val use_cases = new UseCases(fruit_repository)

  @Test
  def t1_migration(): Unit = {
    val migrationResult = PostgreSQLMigration.migrate()
    assert(migrationResult)
  }

  @Test
  def t2_create_fruit(): Unit = {
    val fruit = Fruit("1", "Apple", 1.0f)
    use_cases.create_fruit(fruit)
    assert(fruit_repository.get_fruits().length === 1)
  }

  @Test
  def t3_list_fruits(): Unit = {
    val fruit2 = Fruit("2", "Orange", 2.0f)
    use_cases.create_fruit(fruit2)
    assert(use_cases.get_fruits().length === 2)
  }
}
