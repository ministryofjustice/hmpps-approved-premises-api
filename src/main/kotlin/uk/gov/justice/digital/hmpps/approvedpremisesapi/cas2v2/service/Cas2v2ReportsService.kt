package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UnsubmittedApplicationsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.UnsubmittedApplicationsReportRow
import java.io.OutputStream

@Service
class Cas2v2ReportsService(
  private val submittedApplicationReportRepository: Cas2SubmittedApplicationReportRepository,
  private val applicationStatusUpdatesReportRepository: Cas2ApplicationStatusUpdatesReportRepository,
  private val unsubmittedApplicationsReportRepository: Cas2UnsubmittedApplicationsReportRepository,
) {

  fun createSubmittedApplicationsReport(outputStream: OutputStream) = submittedApplicationReportRepository.generateSubmittedApplicationReportRows(Cas2ServiceOrigin.BAIL.name).map { row ->
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
      applicationOrigin = row.getApplicationOrigin(),
      bailHearingDate = row.getBailHearingDate(),
    )
  }
    .toCas2v2Report(outputStream)

  fun createApplicationStatusUpdatesReport(outputStream: OutputStream) = applicationStatusUpdatesReportRepository
    .generateApplicationStatusUpdatesReportRows(Cas2ServiceOrigin.BAIL.name)
    .map { row ->
      ApplicationStatusUpdatesReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        newStatus = row.getNewStatus(),
        updatedBy = row.getUpdatedBy(),
        updatedAt = row.getUpdatedAt(),
        statusDetails = row.getStatusDetails(),
        applicationOrigin = row.getApplicationOrigin(),
      )
    }
    .toCas2v2Report(outputStream)

  fun createUnsubmittedApplicationsReport(outputStream: OutputStream) = unsubmittedApplicationsReportRepository
    .generateUnsubmittedApplicationsReportRows(Cas2ServiceOrigin.BAIL.name)
    .map { row ->
      UnsubmittedApplicationsReportRow(
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        startedAt = row.getStartedAt(),
        startedBy = row.getStartedBy(),
        applicationOrigin = row.getApplicationOrigin(),
      )
    }
    .toCas2v2Report(outputStream)
}

inline fun <reified T> Iterable<T>.toCas2v2Report(outputStream: OutputStream) = toDataFrame<T>()
  .writeExcel(
    outputStream = outputStream,
    factory = WorkbookFactory.create(true),
  )
