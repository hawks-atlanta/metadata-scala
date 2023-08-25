package org.hawksatlanta.metadata
package fruit.infrastructure

import fruit.domain.Fruit
import fruit.domain.Repository

class InMemoryRepository extends Repository {
  private var fruitsStore: List[Fruit] = List[Fruit]()

  def get_fruits(): List[Fruit] = {
    return fruitsStore
  }

  def save( fruit: Fruit ): Unit = {
    fruitsStore = fruitsStore :+ fruit
  }
}
