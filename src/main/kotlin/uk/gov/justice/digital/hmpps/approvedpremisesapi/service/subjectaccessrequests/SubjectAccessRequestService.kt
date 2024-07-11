package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS1SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS3SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  val objectMapper: ObjectMapper,
  val CAS1SubjectAccessRequestRepository: CAS1SubjectAccessRequestRepository,
  val CAS3SubjectAccessRequestRepository: CAS3SubjectAccessRequestRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCAS1Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val approvedPremisesApplicationsJson = CAS1SubjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val apApplicationTimelineJson = CAS1SubjectAccessRequestRepository.getApprovedPremisesApplicationTimeLineJson(crn, nomsNumber, startDate, endDate)
    val apAssessmentsJson = CAS1SubjectAccessRequestRepository.getApprovedPremisesAssessments(crn, nomsNumber, startDate, endDate)
    val apAssessmentClarificationNotes = CAS1SubjectAccessRequestRepository.getApprovedPremisesAssessmentClarificationNotes(crn, nomsNumber, startDate, endDate)
    val apBookings = CAS1SubjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate)
    val apBookingExtensions = CAS1SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate)
    val apCancellations = CAS1SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate)
    val apBedMoves = CAS1SubjectAccessRequestRepository.bedMoves(crn, nomsNumber, startDate, endDate)
    val appeals = CAS1SubjectAccessRequestRepository.appeals(crn, nomsNumber, startDate, endDate)
    val placementApplications = CAS1SubjectAccessRequestRepository.placementApplications(crn, nomsNumber, startDate, endDate)
    val placementRequests = CAS1SubjectAccessRequestRepository.placementRequests(crn, nomsNumber, startDate, endDate)
    val placementRequirements = CAS1SubjectAccessRequestRepository.placementRequirements(crn, nomsNumber, startDate, endDate)
    val placementRequirementCriteria = CAS1SubjectAccessRequestRepository.placementRequirementsCriteria(crn, nomsNumber, startDate, endDate)
    val offlineApplications = CAS1SubjectAccessRequestRepository.offlineApplications(crn, nomsNumber, startDate, endDate)
    val bookingNotMades = CAS1SubjectAccessRequestRepository.bookingNotMades(crn, nomsNumber, startDate, endDate)
    val domainEvents = CAS1SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate)
    val domainEventMetaData = CAS1SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate)
    val result = """
      {
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
         "PlacementRequirementCriteria": $placementRequirementCriteria,
         "BookingNotMades": $bookingNotMades,
         "DomainEvents": $domainEvents,
         "DomainEventMetadata": $domainEventMetaData
      }
    """.trimIndent()

    if (log.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("CAS1 SAR result is $prettyPrintJson")
    }

    return result
  }

  fun getCAS3Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val temporaryAccommodationApplications = CAS3SubjectAccessRequestRepository.temporaryAccommodationApplications(crn, nomsNumber, startDate, endDate)
    val result = """
      {
        "Applications": $temporaryAccommodationApplications
      }
    """.trimIndent()

    if (log.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("CAS3 SAR result is $prettyPrintJson")
    }
    return result
  }

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?) =
    """
      {
         "ApprovedPremises" : ${getCAS1Result(crn, nomsNumber, startDate, endDate)}
      }
    """.trimIndent()
}
