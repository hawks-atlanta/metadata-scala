package org.hawksatlanta.metadata
package files_metadata

import java.util
import java.util.concurrent.atomic.AtomicBoolean

import io.restassured.response.Response
import io.restassured.RestAssured.`given`

object FilesTestsUtils {
  var wasHttpServerInitializationCalled: AtomicBoolean = new AtomicBoolean(
    false
  )

  def StartHttpServer(): Unit = {
    if (wasHttpServerInitializationCalled.compareAndSet( false, true )) {
      Main.main( Array[String]() )
    }
  }

  def SaveFile( payload: util.HashMap[String, Any] ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
  }

  def ShareFile(
      ownerUUID: String,
      fileUUID: String,
      payload: util.HashMap[String, Any]
  ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .post(
        s"${ ShareFileTestsData.API_PREFIX }/${ ownerUUID }/${ fileUUID }"
      )
  }
}
