package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED
import java.time.Clock
import java.time.LocalDate

@Service
class DataScienceReportsScheduledJob(
  private val dataScienceReportsPublishingService: DataScienceReportsPublishingService,
  private val featureFlagService: FeatureFlagService,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0 9 * * FRI")
  @SchedulerLock(name = "cas_data_science_reports_upload", lockAtMostFor = "60m", lockAtLeastFor = "15m")
  fun generateAndUploadReports() {
    if (!featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED)) {
      log.info("Skipping scheduled CAS Data Science reports generation and upload as feature flag is disabled")
      return
    }

    log.info("Starting scheduled CAS Data Science reports generation and upload")
    val executionDate = LocalDate.now(clock)
    dataScienceReportsPublishingService.generateAndUploadAllReports(executionDate)
    log.info("Finished scheduled CAS Data Science reports generation and upload")
  }
}
