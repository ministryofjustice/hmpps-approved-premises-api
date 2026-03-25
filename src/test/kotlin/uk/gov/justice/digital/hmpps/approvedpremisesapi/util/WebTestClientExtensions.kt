package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.BodyContentSpec.jsonForObject(value: Any): WebTestClient.BodyContentSpec {
  val jsonMapper =
    ApplicationContextProvider.get().getBean(JsonMapper::class.java)

  return this.json(jsonMapper.writeValueAsString(value))
}

inline fun <reified T> WebTestClient.ResponseSpec.bodyAsObject(): T = this.returnResult(T::class.java).responseBody.blockFirst()!!

inline fun <reified T> WebTestClient.ResponseSpec.bodyAsListOfObjects(): List<T> {
  val jsonMapper =
    ApplicationContextProvider.get().getBean(JsonMapper::class.java)

  val rawResponseBody = this.returnResult(String::class.java)
  return jsonMapper.readValue<List<T>>(rawResponseBody.responseBody.blockFirst()!!)
}
