package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.cas1.reporting

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportsScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class Cas1DataScienceReportsScheduledJobTest {

  private val dataScienceReportsPublishingService = mockk<DataScienceReportsPublishingService>(relaxed = true)
  private val featureFlagService = mockk<FeatureFlagService>()
  private val fixedInstant = Instant.parse("2026-10-16T09:00:00Z")
  private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

  private val job = Cas1DataScienceReportsScheduledJob(
    dataScienceReportsPublishingService = dataScienceReportsPublishingService,
    featureFlagService = featureFlagService,
    clock = clock,
  )

  @Test
  fun `generateAndUploadReport executes when feature flag is enabled`() {
    every { featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED) } returns true

    job.generateAndUploadReport()

    verify(exactly = 1) {
      dataScienceReportsPublishingService.generateAndUploadReport(CasDocumentSubType.CAS1, LocalDate.of(2026, 10, 16))
    }
  }

  @Test
  fun `generateAndUploadReport skips execution when feature flag is disabled`() {
    every { featureFlagService.getBooleanFlag(FEATURE_FLAG_DATA_SCIENCE_REPORTS_SCHEDULED_JOB_ENABLED) } returns false

    job.generateAndUploadReport()

    verify(exactly = 0) {
      dataScienceReportsPublishingService.generateAndUploadReport(any<CasDocumentSubType>(), any<LocalDate>())
    }
  }
}
