package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import java.util.UUID

@Component
@SuppressWarnings("TooGenericExceptionThrown")
class Cas1UpdateInboxEventStatusSeedJob(
  private val inboxEventRepository: InboxEventRepository,
) : SeedJob<Cas1UpdateInboxEventStatusCsvRow>(
  requiredHeaders = setOf(
    "inbox_event_id",
    "processed_status",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateInboxEventStatusCsvRow {
    val seedColumns = SeedColumns(columns)
    return Cas1UpdateInboxEventStatusCsvRow(
      inboxEventId = seedColumns.getUuidOrNull("inbox_event_id")!!,
      processedStatus = seedColumns.getStringOrNull("processed_status")!!,
    )
  }

  override fun processRow(row: Cas1UpdateInboxEventStatusCsvRow) {
    val inboxEventId = row.inboxEventId
    val processedStatus = ProcessedStatus.forValue(row.processedStatus)

    val inboxEvent = inboxEventRepository.findById(inboxEventId).orElseThrow {
      error("Unable to find Inbox Event with id: $inboxEventId")
    }

    inboxEvent.processedStatus = processedStatus
    inboxEventRepository.save(inboxEvent)

    log.info("Status updated for inbox event $inboxEventId to $processedStatus.")
  }
}

data class Cas1UpdateInboxEventStatusCsvRow(
  val inboxEventId: UUID,
  val processedStatus: String,
)
