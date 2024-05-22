package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.flipt.api.FliptClient
import io.flipt.api.evaluation.Evaluation
import io.flipt.api.evaluation.models.BooleanEvaluationResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagsLocalOverrideConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FliptFeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

class FeatureFlagServiceTest {

  private val client = mockk<FliptClient>()
  private val sentryService = mockk<SentryService>()

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag if flipt is disabled then return default value if no override`(default: Boolean) {
    val service = FliptFeatureFlagService(
      client = null,
      sentryService = sentryService,
      localOverridesConfig = FeatureFlagsLocalOverrideConfig(),
    )

    val result = service.getBooleanFlag("theKey", default)

    assertThat(result).isEqualTo(default)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag if flipt is disabled then return override if defined`(overrideValue: Boolean) {
    val service = FliptFeatureFlagService(
      client = null,
      sentryService = sentryService,
      localOverridesConfig = FeatureFlagsLocalOverrideConfig(mapOf("theKey" to overrideValue)),
    )

    val result = service.getBooleanFlag("theKey", default = false)

    assertThat(result).isEqualTo(overrideValue)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag return flipt client value`(enabled: Boolean) {
    val evaluation = mockk<Evaluation>()
    val booleanEvaluationResponse = mockk<BooleanEvaluationResponse>()

    every { client.evaluation() } returns evaluation
    every { evaluation.evaluateBoolean(any()) } returns booleanEvaluationResponse
    every { booleanEvaluationResponse.isEnabled } returns enabled

    val service = FliptFeatureFlagService(
      client = client,
      sentryService = sentryService,
      localOverridesConfig = FeatureFlagsLocalOverrideConfig(),
    )

    val result = service.getBooleanFlag("theKey", false)

    assertThat(result).isEqualTo(enabled)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag if exception occurs log it then return default value`(default: Boolean) {
    val exception = RuntimeException("oh dear")
    every { client.evaluation() } throws exception
    every { sentryService.captureException(any()) } returns Unit

    val service = FliptFeatureFlagService(
      client = client,
      sentryService = sentryService,
      localOverridesConfig = FeatureFlagsLocalOverrideConfig(),
    )

    val result = service.getBooleanFlag("theKey", default)

    verify { sentryService.captureException(exception) }

    assertThat(result).isEqualTo(default)
  }
}
