package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled.DeleteOldInboxEventsJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.time.Instant
import java.time.temporal.ChronoUnit

class DeleteOldInboxEventsJobIT : IntegrationTestBase() {
  @Autowired
  lateinit var deleteOldInboxEventsJob: DeleteOldInboxEventsJob

  @Test
  fun `Inbox events older than 90 days are deleted`() {
    val oldEvent = inboxEventEntityFactory.produceAndPersist {
      withCreatedAt(Instant.now().minus(91, ChronoUnit.DAYS))
    }

    val recentEvent = inboxEventEntityFactory.produceAndPersist {
      withCreatedAt(Instant.now().minus(89, ChronoUnit.DAYS))
    }

    assertThat(inboxEventRepository.findById(oldEvent.id)).isPresent
    assertThat(inboxEventRepository.findById(recentEvent.id)).isPresent

    deleteOldInboxEventsJob.deleteOldInboxEvents()

    assertThat(inboxEventRepository.findById(oldEvent.id)).isNotPresent
    assertThat(inboxEventRepository.findById(recentEvent.id)).isPresent
  }
}
