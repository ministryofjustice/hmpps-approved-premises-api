package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UnsubmittedApplicationsReportRepository
import java.io.OutputStream
import java.time.LocalDate

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

data class ApplicationStatusUpdatesReportRow(
  val eventId: String,
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val newStatus: String,
  val updatedAt: String,
  val updatedBy: String,
  val statusDetails: String,
  val applicationOrigin: String,
)

data class SubmittedApplicationReportRow(
  val eventId: String,
  val applicationId: String,
  val personCrn: String,
  val personNoms: String?,
  val referringPrisonCode: String?,
  val preferredAreas: String?,
  val hdcEligibilityDate: LocalDate?,
  val conditionalReleaseDate: LocalDate?,
  val submittedAt: String,
  val submittedBy: String,
  val startedAt: String,
  val applicationOrigin: ApplicationOrigin,
  val bailHearingDate: LocalDate?,
)

data class UnsubmittedApplicationsReportRow(
  val applicationId: String,
  val personCrn: String,
  val personNoms: String?,
  val startedAt: String,
  val startedBy: String,
  val applicationOrigin: ApplicationOrigin,
)
