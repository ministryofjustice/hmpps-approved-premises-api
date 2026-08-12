package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS1SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class Cas1SubjectAccessRequestService(
  val cas1SubjectAccessRequestRepository: CAS1SubjectAccessRequestRepository,
) {

  @SuppressWarnings("CyclomaticComplexMethod")
  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val approvedPremisesApplicationsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val apApplicationTimelineJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationTimeLineJson(crn, nomsNumber, startDate, endDate)
    val apAssessmentsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessments(crn, nomsNumber, startDate, endDate)
    val apAssessmentClarificationNotesJson = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessmentClarificationNotes(crn, nomsNumber, startDate, endDate)

    val apSpaceBookingsJson = cas1SubjectAccessRequestRepository.spaceBookings(crn, nomsNumber, startDate, endDate)
    val domainEventsJson = cas1SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate)

    val placementApplicationsJson = cas1SubjectAccessRequestRepository.placementApplications(crn, nomsNumber, startDate, endDate)
    val placementRequestsJson = cas1SubjectAccessRequestRepository.placementRequests(crn, nomsNumber, startDate, endDate)
    val placementRequirementsJson = cas1SubjectAccessRequestRepository.placementRequirements(crn, nomsNumber, startDate, endDate)
    val placementRequirementCriteriaJson = cas1SubjectAccessRequestRepository.placementRequirementsCriteria(crn, nomsNumber, startDate, endDate)
    val offlineApplicationsJson = cas1SubjectAccessRequestRepository.offlineApplications(crn, nomsNumber, startDate, endDate)
    val bookingNotMadesJson = cas1SubjectAccessRequestRepository.bookingNotMades(crn, nomsNumber, startDate, endDate)
    val appealsJson = cas1SubjectAccessRequestRepository.appeals(crn, nomsNumber, startDate, endDate)

    if (listOf(
        approvedPremisesApplicationsJson,
        apApplicationTimelineJson,
        apAssessmentsJson,
        apAssessmentClarificationNotesJson,
        apSpaceBookingsJson,
        domainEventsJson,
        placementApplicationsJson,
        placementRequestsJson,
        placementRequirementsJson,
        placementRequirementCriteriaJson,
        offlineApplicationsJson,
        bookingNotMadesJson,
        appealsJson,
      ).all { it == null }
    ) {
      return null
    }

    val result = """
      {
         "Applications": ${ approvedPremisesApplicationsJson ?: "[]"},
         "ApplicationTimeline": ${ apApplicationTimelineJson ?: "[]"},
         "Assessments": ${ apAssessmentsJson ?: "[]"},
         "AssessmentClarificationNotes": ${ apAssessmentClarificationNotesJson ?: "[]"},
         "SpaceBookings": ${ apSpaceBookingsJson ?: "[]"},
         "OfflineApplications": ${ offlineApplicationsJson ?: "[]"},
         "Appeals": ${ appealsJson ?: "[]"},
         "PlacementApplications": ${ placementApplicationsJson ?: "[]"},
         "PlacementRequests": ${ placementRequestsJson ?: "[]"},
         "PlacementRequirements": ${ placementRequirementsJson ?: "[]"},
         "PlacementRequirementCriteria": ${ placementRequirementCriteriaJson ?: "[]"},
         "BookingNotMades": ${ bookingNotMadesJson ?: "[]"},
         "DomainEvents": ${ domainEventsJson ?: "[]"}
      }
    """.trimIndent()

    return result
  }
}
