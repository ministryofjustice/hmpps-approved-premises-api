package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED
import java.time.Clock
import java.time.LocalDate

@Service
class Cas3DataScienceReportsScheduledJob(
  private val dataScienceReportsPublishingService: DataScienceReportsPublishingService,
  private val featureFlagService: FeatureFlagService,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 30 9 * * MON")
  @SchedulerLock(name = "cas3_data_science_reports_upload", lockAtMostFor = "60m", lockAtLeastFor = "15m")
  fun generateAndUploadReport() {
    if (!featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED)) {
      log.info("Skipping scheduled CAS3 Data Science report generation and upload as feature flag is disabled")
      return
    }

    log.info("Starting scheduled CAS3 Data Science report generation and upload")
    val executionDate = LocalDate.now(clock)
    dataScienceReportsPublishingService.generateAndUploadReport(CasDocumentSubType.CAS3, executionDate)
    log.info("Finished scheduled CAS3 Data Science report generation and upload")
  }
}
