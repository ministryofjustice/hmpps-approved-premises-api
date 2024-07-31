package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.Charset

fun authorizeUser(): ChainBuilder {
  val jwt = getJwt()

  return exec { session ->
    session.set("access_token", jwt)
  }
}

fun Simulation.SetUp.withAuthorizedUserHttpProtocol() = apply {
  val protocol = http
    .baseUrl(BASE_URL)
    .acceptHeader("*/*")
    .contentTypeHeader("application/json")
    .authorizationHeader("Bearer #{access_token}")

  this.protocols(protocol)
}

private fun getJwt(): String {
  println("Setting up auth ($HMPPS_AUTH_BASE_URL)...")
  val webClient = WebClient.create()

  val savedRequestCookie = webClient.get()
    .uri("$HMPPS_AUTH_BASE_URL/auth/oauth/authorize?response_type=code&state=gatling&client_id=gatling&redirect_uri=http://example.org")
    .exchangeToMono {
      it.printIfError("authorize")
      Mono.justOrEmpty(it.cookies()["savedrequest"]?.get(0))
    }
    .block()

  val jwtCookie = webClient.post()
    .uri("$HMPPS_AUTH_BASE_URL/auth/sign-in")
    .apply {
      if (savedRequestCookie != null) {
        this.cookie("savedrequest", savedRequestCookie.value)
      }
    }
    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
    .bodyValue(
      "redirect_url=${URLEncoder.encode("http://example.org", Charset.defaultCharset())}" +
        "&username=${URLEncoder.encode(USERNAME, Charset.defaultCharset())}" +
        "&password=${URLEncoder.encode(PASSWORD, Charset.defaultCharset())}",
    )
    .exchangeToMono {
      it.printIfError("sign in")
      Mono.justOrEmpty(it.cookies()["jwtSession"]?.get(0))
    }
    .block()

  return jwtCookie?.value ?: throw RuntimeException("Could not get JWT successfully")
}

private fun ClientResponse.printIfError(name: String) {
  if (this.statusCode().isError) {
    println("Could not call '$name' endpoint: ${this.statusCode().value()} ${this.statusCode().reasonPhrase}")
    println()
    println(this.bodyToMono<String>().block())
  }
}
