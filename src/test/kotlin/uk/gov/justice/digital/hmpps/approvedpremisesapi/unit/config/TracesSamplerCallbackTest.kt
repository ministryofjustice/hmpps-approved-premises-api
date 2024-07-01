package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.config

import io.mockk.every
import io.mockk.mockk
import io.sentry.SamplingContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.TracesSamplerCallback

class TracesSamplerCallbackTest {
  private val tracesSamplerCallback = TracesSamplerCallback()

  @ParameterizedTest
  @ValueSource(strings = ["/health", "/health/ping", "/health/readiness", "/health/liveness"])
  fun `Health endpoints are not sampled`(endpoint: String) {
    val mockedContext = mockk<SamplingContext>()

    every { mockedContext.transactionContext.parentSampled } returns null
    every { mockedContext.transactionContext.name } returns "GET $endpoint"

    val result = tracesSamplerCallback.sample(mockedContext)

    assertThat(result).isEqualTo(0.0)
  }

  @Test
  fun `No decision is taken on other endpoints (falls back to sample rate config)`() {
    val mockedContext = mockk<SamplingContext>()

    every { mockedContext.transactionContext.parentSampled } returns null
    every { mockedContext.transactionContext.name } returns "GET /premises"

    val result = tracesSamplerCallback.sample(mockedContext)

    assertThat(result).isNull()
  }

  @Test
  fun `Child traces are sampled at the recommended sample rate of 0_01`() {
    val mockedContext = mockk<SamplingContext>()

    every { mockedContext.transactionContext.parentSampled } returns true
    every { mockedContext.transactionContext.name } returns "GET /premises"

    val result = tracesSamplerCallback.sample(mockedContext)

    assertThat(result).isEqualTo(0.01)
  }
}
