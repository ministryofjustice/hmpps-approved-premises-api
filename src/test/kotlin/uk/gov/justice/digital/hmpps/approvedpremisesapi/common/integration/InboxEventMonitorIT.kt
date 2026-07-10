package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventMonitor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TelemetryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.NoOpTelemetryService

class InboxEventMonitorIT : IntegrationTestBase() {
  @Autowired
  lateinit var inboxEventMonitor: InboxEventMonitor

  @Autowired
  lateinit var noOpTelemetryService: NoOpTelemetryService

  @Test
  fun `stats are published correctly`() {
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PENDING) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PENDING) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.IGNORED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PROCESSED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PROCESSED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PROCESSED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PROCESSED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED_REVIEWED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED_REVIEWED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED_REVIEWED) }
    inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.FAILED_REVIEWED) }

    inboxEventMonitor.publishStats()

    noOpTelemetryService.assertEventRaised(
      TelemetryService.Event(
        name = "InboxEventStats",
        properties = emptyMap(),
        metrics = mapOf(
          "FAILED" to 3.0,
          "FAILED_REVIEWED" to 4.0,
          "IGNORED" to 1.0,
          "PENDING" to 2.0,
          "PROCESSED" to 4.0,
        ),
      ),
    )
  }
}
