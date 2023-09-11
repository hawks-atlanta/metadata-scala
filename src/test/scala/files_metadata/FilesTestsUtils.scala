package org.hawksatlanta.metadata
package files_metadata

import java.util

import io.restassured.response.Response
import io.restassured.RestAssured.`given`

object FilesTestsUtils {
  def SaveFile( payload: util.HashMap[String, Any] ): Response = {
    `given`()
      .port( 8080 )
      .contentType( "application/json" )
      .body( payload )
      .when()
      .post( s"${ SaveFileTestsData.API_PREFIX }" )
  }
}
