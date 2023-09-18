package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class ArchivesMeta(
    uuid: UUID,
    extension: String,
    hashSum: String,
    size: Long,
    ready: Boolean
)

object ArchivesMeta {
  def createNewArchive(
      extension: String,
      hashSum: String,
      size: Long
  ): ArchivesMeta =
    new ArchivesMeta(
      uuid = null,
      extension = extension,
      hashSum = hashSum,
      size = size,
      ready = false
    )
}
