package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.BodyContentSpec.jsonForObject(value: Any): WebTestClient.BodyContentSpec {
  val objectMapper =
    ApplicationContextProvider.get().getBean(ObjectMapper::class.java)

  return this.json(objectMapper.writeValueAsString(value))
}
