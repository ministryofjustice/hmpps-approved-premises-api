package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  val objectMapper: ObjectMapper,
  val subjectAccessRequestRepository: SubjectAccessRequestRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val approvedPremisesApplicationsJson = subjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val apApplicationTimelineJson = subjectAccessRequestRepository.getApprovedPremisesApplicationTimeLineJson(crn, nomsNumber, startDate, endDate)
    val apAssessmentsJson = subjectAccessRequestRepository.getApprovedPremisesAssessments(crn, nomsNumber, startDate, endDate)
    val apAssessmentClarificationNotes = subjectAccessRequestRepository.getApprovedPremisesAssessmentClarificationNotes(crn, nomsNumber, startDate, endDate)
    val apBookings = subjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate)
    val apBookingExtensions = subjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate)
    val apCancellations = subjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate)
    val apBedMoves = subjectAccessRequestRepository.bedMoves(crn, nomsNumber, startDate, endDate)
    val appeals = subjectAccessRequestRepository.appeals(crn, nomsNumber, startDate, endDate)
    val placementApplications = subjectAccessRequestRepository.placementApplications(crn, nomsNumber, startDate, endDate)
    val placementRequests = subjectAccessRequestRepository.placementRequests(crn, nomsNumber, startDate, endDate)
    val placementRequirements = subjectAccessRequestRepository.placementRequirements(crn, nomsNumber, startDate, endDate)
    val placementRequirementCriteria = subjectAccessRequestRepository.placementRequirementsCriteria(crn, nomsNumber, startDate, endDate)
    val offlineApplications = subjectAccessRequestRepository.offlineApplications(crn, nomsNumber, startDate, endDate)

    val result = """
      {
        "approvedPremises" : {
            "Applications": $approvedPremisesApplicationsJson,
            "ApplicationTimeline": $apApplicationTimelineJson,
            "Assessments": $apAssessmentsJson,
            "AssessmentClarificationNotes" : $apAssessmentClarificationNotes,
            "Bookings": $apBookings,
            "OfflineApplications": $offlineApplications,
            "BookingExtensions": $apBookingExtensions,
            "Cancellations": $apCancellations,
            "BedMoves": $apBedMoves,
            "Appeals": $appeals,
            "PlacementApplications": $placementApplications,
            "PlacementRequests": $placementRequests,
            "PlacementRequirements": $placementRequirements,
            "PlacementRequirementCriteria": $placementRequirementCriteria
            
        }
      }
    """.trimIndent()

    if (log.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("SAR result is $prettyPrintJson")
    }

    return result
  }
}
