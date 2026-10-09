package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDataScienceReportPublishingService
import java.time.Clock
import java.time.LocalDate

@Service
class CasDataScienceReportScheduledJob(
  private val casDataScienceReportPublishingService: CasDataScienceReportPublishingService,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0 9 * * MON")
  @SchedulerLock(name = "cas_data_science_reports_upload", lockAtMostFor = "60m", lockAtLeastFor = "15m")
  fun generateAndUploadReports() {
    log.info("Starting scheduled CAS Data Science reports generation and upload")
    val executionDate = LocalDate.now(clock)
    casDataScienceReportPublishingService.generateAndUploadAllReports(executionDate)
    log.info("Finished scheduled CAS Data Science reports generation and upload")
  }
}
