package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2UnsubmittedApplicationsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.UnsubmittedApplicationsReportRow
import java.io.OutputStream

@Service
class Cas2ReportsService(
  private val submittedApplicationReportRepository: Cas2SubmittedApplicationReportRepository,
  private val applicationStatusUpdatesReportRepository: Cas2ApplicationStatusUpdatesReportRepository,
  private val unsubmittedApplicationsReportRepository: Cas2UnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = submittedApplicationReportRepository.generateSubmittedApplicationReportRows().map { row ->
      SubmittedApplicationReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        referringPrisonCode = row.getReferringPrisonCode(),
        preferredAreas = row.getPreferredAreas(),
        hdcEligibilityDate = row.getHdcEligibilityDate(),
        conditionalReleaseDate = row.getConditionalReleaseDate(),
        submittedBy = row.getSubmittedBy(),
        submittedAt = row.getSubmittedAt(),
        startedAt = row.getStartedAt(),
        numberOfTransfers = row.getNumberOfTransfers(),
        applicationOrigin = row.getApplicationOrigin(),
        bailHearingDate = row.getBailHearingDate(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createApplicationStatusUpdatesReport(outputStream: OutputStream) {
    val reportData = applicationStatusUpdatesReportRepository.generateApplicationStatusUpdatesReportRows().map { row ->
      ApplicationStatusUpdatesReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        newStatus = row.getNewStatus(),
        updatedBy = row.getUpdatedBy(),
        updatedAt = row.getUpdatedAt(),
        statusDetails = row.getStatusDetails(),
        numberOfTransfers = row.getNumberOfTransfers(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createUnsubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = unsubmittedApplicationsReportRepository.generateUnsubmittedApplicationsReportRows().map { row ->
      UnsubmittedApplicationsReportRow(
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        startedBy = row.getStartedBy(),
        startedAt = row.getStartedAt(),
        applicationOrigin = row.getApplicationOrigin(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }
}
