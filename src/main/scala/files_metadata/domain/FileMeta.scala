package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class FileMeta(
    uuid: UUID,
    ownerUuid: UUID,
    parentUuid: Option[UUID],
    archiveUuid: Option[UUID],
    volume: String,
    name: String,
    isShared: Boolean
)

object FileMeta {
  def createNewFile(
      ownerUuid: UUID,
      parentUuid: Option[UUID], // Can be empty if it's in the root directory
      name: String
  ): FileMeta = {
    FileMeta(
      uuid = null,
      ownerUuid = ownerUuid,
      parentUuid = parentUuid,
      archiveUuid = null,
      volume = null,
      name = name,
      isShared = false
    )
  }
}
