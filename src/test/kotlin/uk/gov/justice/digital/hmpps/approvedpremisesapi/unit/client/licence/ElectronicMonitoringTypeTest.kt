package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.client.licence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ElectronicMonitoringType

class ElectronicMonitoringTypeTest {
  private val objectMapper: ObjectMapper = jacksonObjectMapper()

  @Test
  fun `can deserialize EXCLUSION_ZONE from uppercase with underscore`() {
    val json = "\"EXCLUSION_ZONE\""
    val result = objectMapper.readValue<ElectronicMonitoringType>(json)
    assertThat(result).isEqualTo(ElectronicMonitoringType.EXCLUSION_ZONE)
  }

  @Test
  fun `can deserialize EXCLUSION_ZONE from lowercase with space`() {
    val json = "\"exclusion zone\""
    val result = objectMapper.readValue<ElectronicMonitoringType>(json)
    assertThat(result).isEqualTo(ElectronicMonitoringType.EXCLUSION_ZONE)
  }

  @Test
  fun `can deserialize CURFEW`() {
    val json = "\"curfew\""
    val result = objectMapper.readValue<ElectronicMonitoringType>(json)
    assertThat(result).isEqualTo(ElectronicMonitoringType.CURFEW)
  }

  @Test
  fun `can deserialize CURFEW case insensitive`() {
    val json = "\"CURFEW\""
    val result = objectMapper.readValue<ElectronicMonitoringType>(json)
    assertThat(result).isEqualTo(ElectronicMonitoringType.CURFEW)
  }

  @Test
  fun `can deserialize LOCATION_MONITORING from mixed case and underscores`() {
    val json = "\"location_monitoring\""
    val result = objectMapper.readValue<ElectronicMonitoringType>(json)
    assertThat(result).isEqualTo(ElectronicMonitoringType.LOCATION_MONITORING)
  }

  @Test
  fun `throws exception for unknown type`() {
    val json = "\"UNKNOWN\""
    val exception = assertThrows<com.fasterxml.jackson.databind.exc.ValueInstantiationException> {
      objectMapper.readValue<ElectronicMonitoringType>(json)
    }
    assertThat(exception.cause?.message).contains("Unknown ElectronicMonitoringType: UNKNOWN")
  }
}
