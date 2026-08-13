package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.CAS3SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class Cas3SubjectAccessRequestService(
  val cas3SubjectAccessRequestRepository: CAS3SubjectAccessRequestRepository,
) {

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val temporaryAccommodationApplicationsJson = cas3SubjectAccessRequestRepository.cas3Applications(crn, nomsNumber, startDate, endDate)
    val temporaryAccommodationAssessmentsJson = cas3SubjectAccessRequestRepository.cas3Assessments(crn, nomsNumber, startDate, endDate)
    val assessmentReferralHistoryNotesJson = cas3SubjectAccessRequestRepository.assessmentReferralHistoryNotes(crn, nomsNumber, startDate, endDate)
    val bookingsJson = cas3SubjectAccessRequestRepository.cas3Bookings(crn, nomsNumber, startDate, endDate)
    val bookingExtensionsJson = cas3SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate)
    val cancellationsJson = cas3SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate)
    val domainEventsJson = cas3SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS3")

    if (listOf(
        temporaryAccommodationApplicationsJson,
        temporaryAccommodationAssessmentsJson,
        assessmentReferralHistoryNotesJson,
        bookingsJson,
        bookingExtensionsJson,
        cancellationsJson,
        domainEventsJson,
      ).all { it == null }
    ) {
      return null
    }

    val result = """
      {
         "Applications": ${ temporaryAccommodationApplicationsJson ?: "[]"},
         "Assessments": ${ temporaryAccommodationAssessmentsJson ?: "[]"},
         "AssessmentReferralHistoryNotes": ${ assessmentReferralHistoryNotesJson ?: "[]"},
         "Bookings": ${ bookingsJson ?: "[]"},
         "BookingExtensions": ${ bookingExtensionsJson ?: "[]"},
         "Cancellations": ${ cancellationsJson ?: "[]"},
         "DomainEvents": ${ domainEventsJson ?: "[]"}
      }
    """.trimIndent()

    return result
  }
}
