package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TelemetryService

@Service
@Primary
class NoOpTelemetryService : TelemetryService {
  private val capturedEvents = mutableListOf<TelemetryService.Event>()

  override fun trackEvent(event: TelemetryService.Event) {
    capturedEvents.add(event)
  }

  fun assertEventRaised(event: TelemetryService.Event) {
    assertThat(capturedEvents).contains(event)
  }

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedEvents.clear()
  }
}
