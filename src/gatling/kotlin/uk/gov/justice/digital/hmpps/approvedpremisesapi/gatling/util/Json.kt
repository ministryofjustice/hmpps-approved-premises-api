package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.javaapi.core.CoreDsl.StringBody

private val objectMapper = lazy { ObjectMapper().findAndRegisterModules() }

fun toJson(value: Any) = StringBody(objectMapper.value.writeValueAsString(value))
