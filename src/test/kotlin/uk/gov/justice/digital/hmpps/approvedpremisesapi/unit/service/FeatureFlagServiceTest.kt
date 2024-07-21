package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SpringConfigFeatureFlagService

class FeatureFlagServiceTest {

  @Test
  fun `getBooleanFlag return false value if no config value`() {
    val service = SpringConfigFeatureFlagService(
      featureFlags = emptyMap(),
    )

    val result = service.getBooleanFlag("theKey")

    assertThat(result).isEqualTo(false)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag return config value if defined`(overrideValue: Boolean) {
    val service = SpringConfigFeatureFlagService(
      featureFlags = mapOf("theKey" to overrideValue),
    )

    val result = service.getBooleanFlag("theKey")

    assertThat(result).isEqualTo(overrideValue)
  }
}
