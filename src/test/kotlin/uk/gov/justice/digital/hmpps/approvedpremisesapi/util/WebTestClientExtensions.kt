package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import kotlin.jvm.java

fun WebTestClient.BodyContentSpec.jsonForObject(value: Any): WebTestClient.BodyContentSpec {
  val jsonMapper = ApplicationContextProvider.get().getBean(JsonMapper::class.java)
  return this.json(jsonMapper.writeValueAsString(value))
}

inline fun <reified T : Any> WebTestClient.ResponseSpec.bodyAsObject(): T = this.returnResult(T::class.java).responseBody.blockFirst()!!

inline fun <reified T : Any> WebTestClient.ResponseSpec.bodyAsListOfObjects(): List<T> {
  val jsonMapper = ApplicationContextProvider.get().getBean(JsonMapper::class.java)
  val rawResponseBody = this.returnResult(String::class.java)
  return jsonMapper.readValue(rawResponseBody.responseBody.blockFirst()!!)
}
