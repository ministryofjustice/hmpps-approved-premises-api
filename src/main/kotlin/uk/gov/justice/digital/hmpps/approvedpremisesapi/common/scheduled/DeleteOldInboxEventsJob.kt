package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventRepository

@Service
class DeleteOldInboxEventsJob(
  private val inboxEventRepository: InboxEventRepository,
  private val transactionTemplate: TransactionTemplate,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Scheduled(cron = "0 0 0 * * *")
  @SchedulerLock(
    name = "DeleteOldInboxEventsJob",
    lockAtMostFor = "PT1M",
    lockAtLeastFor = "PT1M",
  )
  fun deleteOldInboxEvents() {
    transactionTemplate.executeWithoutResult {
      val removedEventCount = inboxEventRepository.deleteInboxEventsOlderThan90Days()
      logger.info("Removed $removedEventCount old inbox events")
    }
  }
}
