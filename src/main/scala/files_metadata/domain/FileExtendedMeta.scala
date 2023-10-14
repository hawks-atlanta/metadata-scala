package org.hawksatlanta.metadata
package files_metadata.domain

import java.util.UUID

case class FileExtendedMeta(
    uuid: UUID,
    ownerUuid: UUID,
    parentUuid: Option[UUID],
    archiveUuid: Option[UUID],
    volume: String,
    name: String,
    extension: String,
    size: Int,
    isReady: Boolean,
    isShared: Boolean
)
