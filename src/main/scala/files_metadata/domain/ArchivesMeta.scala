package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class ArchivesMeta(
    uuid: UUID,
    hashSum: String,
    size: Long,
    ready: Boolean
)

object ArchivesMeta {
  def createNewArchive( hashSum: String, size: Long ): ArchivesMeta =
    new ArchivesMeta( null, hashSum, size, false )
}
