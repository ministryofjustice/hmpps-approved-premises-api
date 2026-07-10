package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TelemetryService
import kotlin.collections.associate

@Service
class InboxEventMonitor(
  private val inboxEventService: InboxEventService,
  private val telemetryService: TelemetryService,
) {

  // every 10 minutes
  @Scheduled(cron = "0 */10 * * * *")
  @SchedulerLock(
    name = "InboxEventPublishStatus",
    lockAtMostFor = "PT1M",
    lockAtLeastFor = "PT1M",
  )
  fun publishStatsOnSchedule() {
    publishStats()
  }

  fun publishStats() {
    val stats = inboxEventService.getStats().associateBy { it.getProcessedStatus() }
    val metrics = ProcessedStatus.entries.associate { status ->
      status.name to (stats[status]?.getCount()?.toDouble() ?: 0.0)
    }

    telemetryService.trackEvent(
      TelemetryService.Event(
        name = "InboxEventStats",
        properties = mapOf(),
        metrics = metrics,
      ),
    )
  }
}
