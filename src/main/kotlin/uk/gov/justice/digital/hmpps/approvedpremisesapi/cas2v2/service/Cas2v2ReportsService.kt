package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UnsubmittedApplicationsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.UnsubmittedApplicationsReportRow
import java.io.OutputStream

@Service
class Cas2v2ReportsService(
  private val cas2v2SubmittedApplicationReportRepository: Cas2v2SubmittedApplicationReportRepository,
  private val cas2v2ApplicationStatusUpdatesReportRepository: Cas2v2ApplicationStatusUpdatesReportRepository,
  private val cas2v2UnsubmittedApplicationsReportRepository: Cas2v2UnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = cas2v2SubmittedApplicationReportRepository.generateSubmittedApplicationReportRows().map { row ->
      SubmittedApplicationReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        applicationOrigin = row.getApplicationOrigin(),
        bailHearingDate = row.getBailHearingDate(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        referringPrisonCode = row.getReferringPrisonCode(),
        preferredAreas = row.getPreferredAreas(),
        hdcEligibilityDate = row.getHdcEligibilityDate(),
        conditionalReleaseDate = row.getConditionalReleaseDate(),
        submittedBy = row.getSubmittedBy(),
        submittedAt = row.getSubmittedAt(),
        startedAt = row.getStartedAt(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createApplicationStatusUpdatesReport(outputStream: OutputStream) {
    val reportData = cas2v2ApplicationStatusUpdatesReportRepository.generateApplicationStatusUpdatesReportRows().map { row ->
      ApplicationStatusUpdatesReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        applicationOrigin = row.getApplicationOrigin().toString(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        newStatus = row.getNewStatus(),
        updatedBy = row.getUpdatedBy(),
        updatedAt = row.getUpdatedAt(),
        statusDetails = row.getStatusDetails(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createUnsubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = cas2v2UnsubmittedApplicationsReportRepository.generateUnsubmittedApplicationsReportRows().map { row ->
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
