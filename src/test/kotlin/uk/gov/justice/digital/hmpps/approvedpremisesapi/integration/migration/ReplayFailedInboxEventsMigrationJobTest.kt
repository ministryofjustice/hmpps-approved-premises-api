package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import java.time.OffsetDateTime

class ReplayFailedInboxEventsMigrationJobTest : MigrationJobTestBase() {

  @Test
  fun `status is updated`() {
    val failed1 = createEvent(ProcessedStatus.FAILED)
    val ignored1 = createEvent(ProcessedStatus.IGNORED)
    val processed1 = createEvent(ProcessedStatus.PROCESSED)
    val pending1 = createEvent(ProcessedStatus.PENDING)

    val failed2 = createEvent(ProcessedStatus.FAILED)
    val ignored2 = createEvent(ProcessedStatus.IGNORED)
    val processed2 = createEvent(ProcessedStatus.PROCESSED)
    val pending2 = createEvent(ProcessedStatus.PENDING)

    migrationJobService.runMigrationJob(MigrationJobType.replayFailedInboxEvents)

    assertThat(inboxEventRepository.findByIdOrNull(failed1.id)!!.processedStatus).isEqualTo(ProcessedStatus.PENDING)
    assertThat(inboxEventRepository.findByIdOrNull(failed1.id)!!.processedAt).isNull()
    assertThat(inboxEventRepository.findByIdOrNull(ignored1.id)!!.processedStatus).isEqualTo(ProcessedStatus.IGNORED)
    assertThat(inboxEventRepository.findByIdOrNull(processed1.id)!!.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(inboxEventRepository.findByIdOrNull(pending1.id)!!.processedStatus).isEqualTo(ProcessedStatus.PENDING)

    assertThat(inboxEventRepository.findByIdOrNull(failed2.id)!!.processedStatus).isEqualTo(ProcessedStatus.PENDING)
    assertThat(inboxEventRepository.findByIdOrNull(failed2.id)!!.processedAt).isNull()
    assertThat(inboxEventRepository.findByIdOrNull(ignored2.id)!!.processedStatus).isEqualTo(ProcessedStatus.IGNORED)
    assertThat(inboxEventRepository.findByIdOrNull(processed2.id)!!.processedStatus).isEqualTo(ProcessedStatus.PROCESSED)
    assertThat(inboxEventRepository.findByIdOrNull(pending2.id)!!.processedStatus).isEqualTo(ProcessedStatus.PENDING)
  }

  private fun createEvent(status: ProcessedStatus) = inboxEventEntityFactory.produceAndPersist {
    withEventType("test")
    withEventOccurredAt(OffsetDateTime.now())
    withPayload("{}")
    withProcessedStatus(status)
    withProcessedAt(null)
  }
}
