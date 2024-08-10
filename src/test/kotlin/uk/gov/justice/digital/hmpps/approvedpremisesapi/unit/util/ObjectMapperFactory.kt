package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object ObjectMapperFactory {
  fun createRuntimeLikeObjectMapper() = ObjectMapper().apply {
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

}