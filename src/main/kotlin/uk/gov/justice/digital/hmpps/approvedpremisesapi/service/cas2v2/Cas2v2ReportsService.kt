package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UnsubmittedApplicationsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2.UnsubmittedApplicationsReportRow
import java.io.OutputStream

@Service
class Cas2v2ReportsService(
  private val cas2v2SubmittedApplicationReportRepository: Cas2v2SubmittedApplicationReportRepository,
  private val cas2v2ApplicationStatusUpdatesReportRepository: Cas2v2ApplicationStatusUpdatesReportRepository,
  private val cas2v2UnsubmittedApplicationsReportRepository: Cas2v2UnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = cas2v2SubmittedApplicationReportRepository.generateSubmittedApplicationReportRows().map { row ->

      val eventId = row.getId()
      val applicationId = row.getApplicationId()
      val applicationOrigin = row.getApplicationOrigin() ?: ApplicationOrigin.homeDetentionCurfew
      val personCrn = row.getPersonCrn()
      val personNoms = row.getPersonNoms()
      val referringPrisonCode = row.getReferringPrisonCode()
      val preferredAreas = row.getPreferredAreas()
      val hdcEligibilityDate = row.getHdcEligibilityDate()
      val conditionalReleaseDate = row.getConditionalReleaseDate()
      val submittedBy = row.getSubmittedBy()
      val submittedAt = row.getSubmittedAt()
      val startedAt = row.getStartedAt() ?: ""

      SubmittedApplicationReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        applicationOrigin = row.getApplicationOrigin() ?: ApplicationOrigin.homeDetentionCurfew,
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
        applicationOrigin = row.getApplicationOrigin(),
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
