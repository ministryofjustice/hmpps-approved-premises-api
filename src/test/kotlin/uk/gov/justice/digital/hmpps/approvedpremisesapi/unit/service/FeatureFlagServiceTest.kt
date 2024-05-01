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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

class FeatureFlagServiceTest {

  val client = mockk<FliptClient>()
  val sentryService = mockk<SentryService>()

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag if flipt is disabled then return default value`(default: Boolean) {
    val service = FeatureFlagService(
      client = null,
      sentryService,
    )

    val result = service.getBooleanFlag("theKey", default)

    assertThat(result).isEqualTo(default)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag return flipt client value`(enabled: Boolean) {
    val evaluation = mockk<Evaluation>()
    val booleanEvaluationResponse = mockk<BooleanEvaluationResponse>()

    every { client.evaluation() } returns evaluation
    every { evaluation.evaluateBoolean(any()) } returns booleanEvaluationResponse
    every { booleanEvaluationResponse.isEnabled } returns enabled

    val service = FeatureFlagService(client, sentryService)

    val result = service.getBooleanFlag("theKey", false)

    assertThat(result).isEqualTo(enabled)
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `getBooleanFlag if exception occurs log it then return default value`(default: Boolean) {
    val exception = RuntimeException("oh dear")
    every { client.evaluation() } throws exception
    every { sentryService.captureException(any()) } returns Unit

    val service = FeatureFlagService(client, sentryService)

    val result = service.getBooleanFlag("theKey", default)

    verify { sentryService.captureException(exception) }

    assertThat(result).isEqualTo(default)
  }
}
