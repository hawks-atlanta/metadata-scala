package org.hawksatlanta.metadata
package fruit.domain

trait Repository {
  def get_fruits(): List[Fruit]
  def save( fruit: Fruit ): Unit
}
