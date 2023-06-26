package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.javaapi.core.CoreDsl.StringBody

fun toJson(value: Any) = StringBody(ObjectMapper().writeValueAsString(value))
