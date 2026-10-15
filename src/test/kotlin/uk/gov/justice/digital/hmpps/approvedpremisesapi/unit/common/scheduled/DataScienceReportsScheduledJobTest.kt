package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.common.scheduled

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled.DataScienceReportsScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DataScienceReportsScheduledJobTest {

  private val dataScienceReportsPublishingService = mockk<DataScienceReportsPublishingService>(relaxed = true)
  private val featureFlagService = mockk<FeatureFlagService>()
  private val fixedInstant = Instant.parse("2026-10-15T09:00:00Z")
  private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

  private val job = DataScienceReportsScheduledJob(
    dataScienceReportsPublishingService = dataScienceReportsPublishingService,
    featureFlagService = featureFlagService,
    clock = clock,
  )

  @Test
  fun `generateAndUploadReports executes when feature flag is enabled`() {
    every { featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED) } returns true

    job.generateAndUploadReports()

    verify(exactly = 1) {
      dataScienceReportsPublishingService.generateAndUploadAllReports(LocalDate.of(2026, 10, 15))
    }
  }

  @Test
  fun `generateAndUploadReports skips execution when feature flag is disabled`() {
    every { featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED) } returns false

    job.generateAndUploadReports()

    verify(exactly = 0) {
      dataScienceReportsPublishingService.generateAndUploadAllReports(any())
    }
  }
}
