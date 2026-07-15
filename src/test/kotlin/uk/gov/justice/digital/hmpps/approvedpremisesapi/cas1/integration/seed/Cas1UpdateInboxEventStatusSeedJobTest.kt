package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateInboxEventStatusCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.util.UUID
import kotlin.collections.forEach

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1UpdateInboxEventStatusSeedJobTest : SeedTestBase() {

  @Test
  fun `Throws exception if event id does not exist, rolls back transaction and logs error`() {
    val inboxEvent = inboxEventEntityFactory.produceAndPersist { withProcessedStatus(ProcessedStatus.PROCESSED) }

    val randomUUID = UUID.randomUUID()

    seed(
      SeedFileType.approvedPremisesUpdateInboxEventStatus,
      rowsToCsv(
        listOf(
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent.id,
            processedStatus = ProcessedStatus.PENDING.name,
          ),
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = randomUUID,
            processedStatus = ProcessedStatus.PENDING.name,
          ),
        ),
      ),
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message!!.contains("Unable to find Inbox Event with id: $randomUUID")
      }

    val event1 = inboxEventRepository.findById(inboxEvent.id).get()
    assertThat(event1.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
  }

  @Test
  fun `Error if event exists and new status is invalid and don't update any statuses`() {
    val inboxEvent = inboxEventEntityFactory.produceAndPersistMultiple(2) { withProcessedStatus(ProcessedStatus.PROCESSED) }

    seed(
      SeedFileType.approvedPremisesUpdateInboxEventStatus,
      rowsToCsv(
        listOf(
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[0].id,
            processedStatus = ProcessedStatus.PENDING.name,
          ),
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[1].id,
            processedStatus = "invalid_status",
          ),
        ),
      ),
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message!!.contains("Error on row 2: Collection contains no element matching the predicate.")
      }

    val event1 = inboxEventRepository.findById(inboxEvent[0].id).get()
    assertThat(event1.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
  }

  @Test
  fun `Run successfully and update (lowercase) statuses if ids exist`() {
    val inboxEvent = inboxEventEntityFactory.produceAndPersistMultiple(5) { withProcessedStatus(ProcessedStatus.PROCESSED) }

    seed(
      SeedFileType.approvedPremisesUpdateInboxEventStatus,
      rowsToCsv(
        listOf(
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[2].id,
            processedStatus = "pending",
          ),
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[4].id,
            processedStatus = "failed_reviewed",
          ),
        ),
      ),
    )

    val eventsById = inboxEventRepository
      .findAllById(listOf(inboxEvent[0].id, inboxEvent[1].id, inboxEvent[2].id, inboxEvent[3].id, inboxEvent[4].id))
      .associateBy { it.id }

    assertThat(eventsById[inboxEvent[0].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[1].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[2].id]?.processedStatus).isEqualTo(ProcessedStatus.PENDING)
    assertThat(eventsById[inboxEvent[3].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[4].id]?.processedStatus).isEqualTo(ProcessedStatus.FAILED_REVIEWED)
  }

  @Test
  fun `Run successfully and update (uppercase) statuses if ids exist`() {
    val inboxEvent = inboxEventEntityFactory.produceAndPersistMultiple(5) { withProcessedStatus(ProcessedStatus.PROCESSED) }

    seed(
      SeedFileType.approvedPremisesUpdateInboxEventStatus,
      rowsToCsv(
        listOf(
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[2].id,
            processedStatus = "PENDING",
          ),
          Cas1UpdateInboxEventStatusCsvRow(
            inboxEventId = inboxEvent[4].id,
            processedStatus = "FAILED_REVIEWED",
          ),
        ),
      ),
    )

    val eventsById = inboxEventRepository
      .findAllById(listOf(inboxEvent[0].id, inboxEvent[1].id, inboxEvent[2].id, inboxEvent[3].id, inboxEvent[4].id))
      .associateBy { it.id }

    assertThat(eventsById[inboxEvent[0].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[1].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[2].id]?.processedStatus).isEqualTo(ProcessedStatus.PENDING)
    assertThat(eventsById[inboxEvent[3].id]?.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(eventsById[inboxEvent[4].id]?.processedStatus).isEqualTo(ProcessedStatus.FAILED_REVIEWED)
  }

  private fun rowsToCsv(rows: List<Cas1UpdateInboxEventStatusCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "inbox_event_id",
        "processed_status",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.inboxEventId.toString())
        .withQuotedField(it.processedStatus)
        .newRow()
    }

    return builder.build()
  }
}
