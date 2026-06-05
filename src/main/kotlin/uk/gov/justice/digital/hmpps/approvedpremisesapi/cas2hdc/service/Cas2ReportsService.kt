package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.UnsubmittedApplicationsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UnsubmittedApplicationsReportRepository
import java.io.OutputStream
import java.time.format.DateTimeFormatter

@Service
class Cas2ReportsService(
  private val submittedApplicationReportRepository: Cas2SubmittedApplicationReportRepository,
  private val applicationStatusUpdatesReportRepository: Cas2ApplicationStatusUpdatesReportRepository,
  private val unsubmittedApplicationsReportRepository: Cas2UnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = submittedApplicationReportRepository.generateSubmittedApplicationReportRows(Cas2ServiceOrigin.HDC.name).map { row ->
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
        numberOfLocationTransfers = row.getNumberOfLocationTransfers(),
        numberOfPomTransfers = row.getNumberOfPomTransfers(),
        applicationOrigin = row.getApplicationOrigin(),
        bailHearingDate = row.getBailHearingDate()?.format(DateTimeFormatter.ISO_LOCAL_DATE),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createApplicationStatusUpdatesReport(outputStream: OutputStream) {
    val reportData = applicationStatusUpdatesReportRepository.generateApplicationStatusUpdatesReportRows(Cas2ServiceOrigin.HDC.name).map { row ->
      ApplicationStatusUpdatesReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        newStatus = row.getNewStatus(),
        updatedBy = row.getUpdatedBy(),
        updatedAt = row.getUpdatedAt(),
        statusDetails = row.getStatusDetails(),
        numberOfLocationTransfers = row.getNumberOfLocationTransfers(),
        numberOfPomTransfers = row.getNumberOfPomTransfers(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createUnsubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = unsubmittedApplicationsReportRepository.generateUnsubmittedApplicationsReportRows(Cas2ServiceOrigin.HDC.name).map { row ->
      UnsubmittedApplicationsReportRow(
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        startedBy = row.getStartedBy(),
        startedAt = row.getStartedAt(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }
}
