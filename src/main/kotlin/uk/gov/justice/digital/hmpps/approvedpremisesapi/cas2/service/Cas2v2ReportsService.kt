package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationStatusUpdatesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
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
      cohort = row.getCohort()?.toDescription(),
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
        cohort = row.getCohort()?.toDescription(),
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
        cohort = row.getCohort()?.toDescription(),
      )
    }
    .toCas2v2Report(outputStream)

  private fun Cas2Cohort.toDescription() = when (this) {
    Cas2Cohort.HDC -> error("report should not include HDC")
    Cas2Cohort.PRISON_BAIL -> "Prison Bail"
    Cas2Cohort.COURT_BAIL -> "Court Bail"
    Cas2Cohort.ATCR -> "Alternative to custodial recall"
    Cas2Cohort.HCRD -> "Homeless at conditional release date"
    Cas2Cohort.HEFR -> "Homeless at end of fixed-term recall"
    Cas2Cohort.ISC -> "Intensive supervision courts"
    Cas2Cohort.RARR -> "Risk Assessed Recall Review"
    Cas2Cohort.FROM_AP -> "Referral from Approved Premises"
  }

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
    val cohort: String?,
  )

  data class SubmittedApplicationReportRow(
    val eventId: String,
    val applicationId: String,
    val personCrn: String,
    val personNoms: String,
    val referringPrisonCode: String?,
    val preferredAreas: String?,
    val hdcEligibilityDate: LocalDate?,
    val conditionalReleaseDate: LocalDate?,
    val submittedAt: String,
    val submittedBy: String,
    val startedAt: String,
    val applicationOrigin: ApplicationOrigin,
    val bailHearingDate: LocalDate?,
    val cohort: String?,
  )

  data class UnsubmittedApplicationsReportRow(
    val applicationId: String,
    val personCrn: String,
    val personNoms: String?,
    val startedAt: String,
    val startedBy: String,
    val applicationOrigin: ApplicationOrigin,
    val cohort: String?,
  )
}

inline fun <reified T> Iterable<T>.toCas2v2Report(outputStream: OutputStream) = toDataFrame<T>()
  .writeExcel(
    outputStream = outputStream,
    factory = WorkbookFactory.create(true),
  )
