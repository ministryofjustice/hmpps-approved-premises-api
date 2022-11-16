package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.jamsesso.jsonlogic.JsonLogic
import org.springframework.stereotype.Service

@Service
class JsonLogicService(private val objectMapper: ObjectMapper) {
  private val jsonLogic = JsonLogic()

  fun resolveBoolean(jsonLogicRule: String, data: String): Boolean = resolve(jsonLogicRule, data)

  private inline fun <reified T> resolve(jsonLogicRule: String, data: String): T {
    val suitableData = objectMapper.readValue<Map<*, *>>(data)

    val result = jsonLogic.apply(jsonLogicRule, suitableData)

    if (result !is T) {
      throw RuntimeException("Expected ${T::class.qualifiedName} but got ${result::class.qualifiedName}")
    }

    return result
  }
}
