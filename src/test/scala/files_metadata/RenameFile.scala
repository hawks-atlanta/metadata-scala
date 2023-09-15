package org.hawksatlanta.metadata
package files_metadata

object RenameFileTestsData {
  val API_PREFIX: String = "/api/v1/files/rename"
  val USER_UUID: UUID    = UUID.randomUUID()

  var sharedFileUUID: UUID   = _
  var unsharedFileUUID: UUID = _
}
class RenameFile {}
