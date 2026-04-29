package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object JsonMapperFactory {
  /**
   * Creates a Jackson2 Json Mapper that matches the
   * mapper provided by Spring at runtime
   */
  fun createJackson2JsonMapper() = com.fasterxml.jackson.databind.json.JsonMapper().apply {
    disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    registerModule(com.fasterxml.jackson.datatype.jdk8.Jdk8Module())
    registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
    registerKotlinModule()
  }

  /**
   * Creates a Jackson3 Json Mapper that matches the
   * mapper provided by Spring at runtime
   */
  fun createJackson3JsonMapper(): JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .defaultDateFormat(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"))
    .build()
}
