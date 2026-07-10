package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventRepository

@Component
class ReplayFailedInboxEventsMigrationJob(
  val inboxEventRepository: InboxEventRepository,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    val updateCount = inboxEventRepository.setFailedAsPending()

    log.info("Have set $updateCount 'failed' entries to 'pending'")
  }
}
