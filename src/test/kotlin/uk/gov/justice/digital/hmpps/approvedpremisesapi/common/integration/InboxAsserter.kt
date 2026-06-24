package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus

@Component
class InboxAsserter(
  val inboxEventRepository: InboxEventRepository,
) {

  fun waitForPendingCount(expectedCount: Int) = Awaitility.await().untilAsserted { assertPendingCount(expectedCount) }

  fun assertFailedCount(expectedCount: Int) = assertCount(expectedCount, ProcessedStatus.FAILED)

  fun assertPendingCount(expectedCount: Int) = assertCount(expectedCount, ProcessedStatus.PENDING)

  fun assertProcessedCount(expectedCount: Int) = assertCount(expectedCount, ProcessedStatus.PROCESSED)

  fun assertNotProcessedCount(expectedCount: Int) = assertCount(expectedCount, ProcessedStatus.NOT_PROCESSED)

  private fun assertCount(expectedCount: Int, status: ProcessedStatus) {
    val processed = inboxEventRepository.findAllByProcessedStatus(status, PageRequest.ofSize(Int.MAX_VALUE))
    assertThat(processed).hasSize(expectedCount)
  }
}
