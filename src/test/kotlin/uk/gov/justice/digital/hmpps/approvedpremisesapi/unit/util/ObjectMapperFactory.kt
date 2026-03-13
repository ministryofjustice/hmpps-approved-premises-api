package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object ObjectMapperFactory {
  fun createRuntimeLikeObjectMapper1() = ObjectMapper().apply {
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }
  fun createRuntimeLikeObjectMapper() = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .defaultDateFormat(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"))
    .build()
}
