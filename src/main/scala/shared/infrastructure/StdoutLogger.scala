package org.hawksatlanta.metadata
package shared.infrastructure
import java.util.Date

import ujson.Obj

object StdoutLogger {
  def logAndReturnEndpointResponse(
      endpoint: String,
      response: cask.Response[Obj]
  ): cask.Response[Obj] = {
    val currentDate = new Date()

    val logMessage =
      s"[$currentDate] [Response] $endpoint => ${ response.statusCode }"
    println( logMessage )

    response
  }

  def logCaughtException(
      exception: Throwable
  ): Unit = {
    val currentDate = new Date()

    val logMessage = s"[$currentDate] [Error] => $exception"
    println( logMessage )
  }
}
