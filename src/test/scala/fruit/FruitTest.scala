package org.hawksatlanta.metadata
package fruit

import fruit.application.UseCases
import fruit.domain.Fruit
import fruit.infrastructure.InMemoryRepository

import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

class FruitTest extends JUnitSuite {
  val fruit_repository = new InMemoryRepository()
  val use_cases = new UseCases(fruit_repository)

  @Test
  def test_create_fruit(): Unit = {
    val fruit = Fruit("1", "Apple", 1.0f)
    use_cases.create_fruit(fruit)
    assert(fruit_repository.get_fruits().length === 1)
  }

  @Test
  def test_get_fruits(): Unit = {
    val fruit = Fruit("1", "Apple", 1.0f)
    use_cases.create_fruit(fruit)

    val fruit2 = Fruit("2", "Orange", 2.0f)
    use_cases.create_fruit(fruit2)

    assert(use_cases.get_fruits().length === 2)
  }
}
