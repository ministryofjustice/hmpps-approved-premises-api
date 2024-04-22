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

fun authorizeUser(serviceName: String): ChainBuilder {
  var username: String
  var password: String

  when (serviceName) {
    "cas1" -> {
      username = CAS1_USERNAME
      password = CAS1_PASSWORD
    }
    "cas2" -> {
      username = CAS2_USERNAME
      password = CAS2_PASSWORD
    }
    "cas3" -> {
      username = CAS3_USERNAME
      password = CAS3_PASSWORD
    }
    else -> {
      throw IllegalArgumentException("Invalid service name: $serviceName")
    }
  }

  val jwt = getJwt(username, password)

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

private fun getJwt(username: String, password: String): String {
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
        "&username=${URLEncoder.encode(username, Charset.defaultCharset())}" +
        "&password=${URLEncoder.encode(password, Charset.defaultCharset())}",
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
