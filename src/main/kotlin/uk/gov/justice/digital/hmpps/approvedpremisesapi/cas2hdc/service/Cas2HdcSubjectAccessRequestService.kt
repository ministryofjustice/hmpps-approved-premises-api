package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class Cas2HdcSubjectAccessRequestService(
  val cas2SubjectAccessRequestRepository: Cas2SubjectAccessRequestRepository,
) {

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val applicationsJson = cas2SubjectAccessRequestRepository.getApplicationsJson(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val applicationNotesJson =
      cas2SubjectAccessRequestRepository.getApplicationNotes(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val statusUpdatesJson = cas2SubjectAccessRequestRepository.getStatusUpdates(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val statusUpdateDetailsJson =
      cas2SubjectAccessRequestRepository.getStatusUpdateDetails(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val assessmentsJson = cas2SubjectAccessRequestRepository.getAssessments(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val domainEventsJson = cas2SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS2")

    if (listOf(
        applicationsJson,
        applicationNotesJson,
        statusUpdatesJson,
        statusUpdateDetailsJson,
        assessmentsJson,
        domainEventsJson,

      ).all { it == null }
    ) {
      return null
    }

    val result = """
      {
         "Applications": ${ applicationsJson ?: "[]"},
         "ApplicationNotes": ${ applicationNotesJson ?: "[]"},
         "Assessments": ${ assessmentsJson ?: "[]"},
         "StatusUpdates": ${ statusUpdatesJson ?: "[]"},
         "StatusUpdateDetails": ${ statusUpdateDetailsJson ?: "[]"},
         "DomainEvents": ${ domainEventsJson ?: "[]"},
      }
    """.trimIndent()

    return result
  }
}
