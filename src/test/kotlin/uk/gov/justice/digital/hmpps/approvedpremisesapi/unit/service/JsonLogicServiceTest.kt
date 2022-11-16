package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonLogicService

class JsonLogicServiceTest {
  private val jsonLogicService = JsonLogicService(jacksonObjectMapper())

  @Test
  fun `resolveBoolean returns true for truthy rule-data combination`() {
    val rule = """
      {
        "==": [
          { "var": "one" }, 
          { "var": "two" }
        ]
      }
      """

    val data = SimpleClass(
      one = 5,
      two = 5
    )

    val serializedData = jacksonObjectMapper().writeValueAsString(data)

    assertThat(jsonLogicService.resolveBoolean(rule, serializedData)).isTrue
  }

  @Test
  fun `resolveBoolean returns false for falsy rule-data combination`() {
    val rule = """
      {
        "!=": [
          { "var": "one" }, 
          { "var": "two" }
        ]
      }
      """

    val data = SimpleClass(
      one = 5,
      two = 5
    )

    val serializedData = jacksonObjectMapper().writeValueAsString(data)

    assertThat(jsonLogicService.resolveBoolean(rule, serializedData)).isFalse
  }
}

data class SimpleClass(
  val one: Int,
  val two: Int
)
