package uk.gov.justice.digital.hmpps.approvedpremisesapi.cmd

import com.fasterxml.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.system.exitProcess

@SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
object AdminJobClient {

  fun invokeEndpoint(
    path: String,
    map: Map<String, String>,
  ) {
    try {
      val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

      val jsonBody = JsonMapper().writeValueAsString(map)

      val url = "http://127.0.0.1:8080/$path"
      println("Invoking $url with arguments $map")

      val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build()

      val response = client.send(
        request,
        HttpResponse.BodyHandlers.ofString(),
      )

      if (response.statusCode() !in 200..299) {
        System.err.println(
          "Request failed (${response.statusCode()}):\n${response.body()}",
        )
        exitProcess(1)
      }

      println("Job triggered successfully.")
    } catch (ex: Exception) {
      System.err.println("Request failed: ${ex.message}")
      exitProcess(1)
    }
  }
}
