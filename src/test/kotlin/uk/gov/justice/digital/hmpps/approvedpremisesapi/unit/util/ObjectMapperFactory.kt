package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object ObjectMapperFactory {
  fun createRuntimeLikeObjectMapper() = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .defaultDateFormat(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"))
    .build()
}
