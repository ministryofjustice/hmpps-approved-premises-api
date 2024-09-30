package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class Cas1ExpiredApplicationsScheduledJob {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0/30 * * * ?")
  @SchedulerLock(name = "cas1_expire_applications", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun expireApplications() {
    log.info("Checking for expired applications (Dry Run)")
  }
}
