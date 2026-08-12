package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2v2SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class Cas2SubjectAccessRequestService(
  val cas2v2SubjectAccessRequestRepository: Cas2v2SubjectAccessRequestRepository,
) {

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val applicationsJson = cas2v2SubjectAccessRequestRepository.getApplicationsJson(crn, nomsNumber, startDate, endDate)
    val applicationNotesJson = cas2v2SubjectAccessRequestRepository.getApplicationNotes(crn, nomsNumber, startDate, endDate)
    val statusUpdatesJson = cas2v2SubjectAccessRequestRepository.getStatusUpdates(crn, nomsNumber, startDate, endDate)
    val statusUpdateDetailsJson =
      cas2v2SubjectAccessRequestRepository.getStatusUpdateDetails(crn, nomsNumber, startDate, endDate)
    val assessmentsJson = cas2v2SubjectAccessRequestRepository.getAssessments(crn, nomsNumber, startDate, endDate)
    val domainEventsJson = cas2v2SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS2V2")

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
         "DomainEvents": ${ domainEventsJson ?: "[]"}
      }
    """.trimIndent()

    return result
  }
}
