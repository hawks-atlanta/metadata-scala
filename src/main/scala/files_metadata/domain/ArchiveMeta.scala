package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class ArchiveMeta(
    uuid: UUID,
    extension: String,
    size: Int,
    ready: Boolean
)

object ArchiveMeta {
  def createNewArchive(
      extension: String,
      size: Int
  ): ArchiveMeta =
    new ArchiveMeta(
      uuid = null,
      ready = false,
      extension = extension,
      size = size
    )
}
