package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class ArchivesMeta(
    uuid: UUID,
    extension: String,
    size: Long,
    ready: Boolean
)

object ArchivesMeta {
  def createNewArchive(
      extension: String,
      size: Long
  ): ArchivesMeta =
    new ArchivesMeta(
      uuid = null,
      ready = false,
      extension = extension,
      size = size
    )
}
