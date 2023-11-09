package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.Session

private val objectMapper = lazy { ObjectMapper().findAndRegisterModules() }

fun toJson(value: Any) = StringBody(objectMapper.value.writeValueAsString(value))

fun toJson(f: (Session) -> Any) = StringBody { session -> objectMapper.value.writeValueAsString(f(session)) }
