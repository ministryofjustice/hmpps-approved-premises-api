package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import java.util.UUID

@Component
@SuppressWarnings("TooGenericExceptionThrown")
class UpdateInboxEventStatusSeedJob(
  private val inboxEventRepository: InboxEventRepository,
) : SeedJob<UpdateInboxEventStatusCsvRow>(
  requiredHeaders = setOf(
    "inbox_event_id",
    "processed_status",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): UpdateInboxEventStatusCsvRow {
    val seedColumns = SeedColumns(columns)
    return UpdateInboxEventStatusCsvRow(
      inboxEventId = seedColumns.getUuidOrNull("inbox_event_id")!!,
      processedStatus = seedColumns.getStringOrNull("processed_status")!!,
    )
  }

  override fun processRow(row: UpdateInboxEventStatusCsvRow) {
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

data class UpdateInboxEventStatusCsvRow(
  val inboxEventId: UUID,
  val processedStatus: String,
)
