package org.hawksatlanta.metadata
package fruit.application

import fruit.domain.{Fruit, Repository}

class UseCases {
  private var repository: Repository = _;

  def this(repository: Repository) {
    this()
    this.repository = repository
  }

  def get_fruits(): List[Fruit] = {
    return repository.get_fruits()
  }

  def create_fruit(fruit: Fruit): Unit = {
    repository.save(fruit)
  }
}
