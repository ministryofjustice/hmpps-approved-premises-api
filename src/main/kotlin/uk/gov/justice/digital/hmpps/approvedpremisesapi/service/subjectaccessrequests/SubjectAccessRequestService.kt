package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS1SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS2SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS3SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  val objectMapper: ObjectMapper,
  val cas1SubjectAccessRequestRepository: CAS1SubjectAccessRequestRepository,
  val cas2SubjectAccessRequestRepository: CAS2SubjectAccessRequestRepository,
  val cas3SubjectAccessRequestRepository: CAS3SubjectAccessRequestRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCAS1Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val approvedPremisesApplicationsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val apApplicationTimelineJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationTimeLineJson(crn, nomsNumber, startDate, endDate)
    val apAssessmentsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessments(crn, nomsNumber, startDate, endDate)
    val apAssessmentClarificationNotes = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessmentClarificationNotes(crn, nomsNumber, startDate, endDate)

    val apBookings = cas1SubjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate)
    val apBookingExtensions = cas1SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate)
    val apCancellations = cas1SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate)
    val apBedMoves = cas1SubjectAccessRequestRepository.bedMoves(crn, nomsNumber, startDate, endDate)
    val domainEvents = cas1SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate)
    val domainEventMetaData = cas1SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate)

    val placementApplications = cas1SubjectAccessRequestRepository.placementApplications(crn, nomsNumber, startDate, endDate)
    val placementRequests = cas1SubjectAccessRequestRepository.placementRequests(crn, nomsNumber, startDate, endDate)
    val placementRequirements = cas1SubjectAccessRequestRepository.placementRequirements(crn, nomsNumber, startDate, endDate)
    val placementRequirementCriteria = cas1SubjectAccessRequestRepository.placementRequirementsCriteria(crn, nomsNumber, startDate, endDate)
    val offlineApplications = cas1SubjectAccessRequestRepository.offlineApplications(crn, nomsNumber, startDate, endDate)
    val bookingNotMades = cas1SubjectAccessRequestRepository.bookingNotMades(crn, nomsNumber, startDate, endDate)
    val appeals = cas1SubjectAccessRequestRepository.appeals(crn, nomsNumber, startDate, endDate)

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
    val temporaryAccommodationApplications = cas3SubjectAccessRequestRepository.temporaryAccommodationApplications(crn, nomsNumber, startDate, endDate)
    val temporaryAccommodationAssessments = cas3SubjectAccessRequestRepository.temporaryAccommodationAssessments(crn, nomsNumber, startDate, endDate)
    val assessmentReferralHistoryNotes = cas3SubjectAccessRequestRepository.assessmentReferralHistoryNotes(crn, nomsNumber, startDate, endDate)
    val bookings = cas3SubjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val bookingExtensions = cas3SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val cancellations = cas3SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val domainEvents = cas3SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS3")
    val domainEventMetaData = cas3SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate, "CAS3")

    val result = """
      {
        "Applications": $temporaryAccommodationApplications,
        "Assessments": $temporaryAccommodationAssessments,
        "AssessmentReferralHistoryNotes": $assessmentReferralHistoryNotes,
        "Bookings": $bookings,
        "BookingExtensions": $bookingExtensions,
        "Cancellations": $cancellations,
        "DomainEvents": $domainEvents,
        "DomainEventMetadata": $domainEventMetaData
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

  fun getCAS2Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val applications = cas2SubjectAccessRequestRepository.getApplicationsJson(crn, nomsNumber, startDate, endDate)
    val assessments = cas2SubjectAccessRequestRepository.getAssessments(crn, nomsNumber, startDate, endDate)
    val result = """
      {
       "Applications": $applications,
       "Assessments": $assessments
       }
    """.trimIndent()

    if (log.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("CAS2 SAR result is $prettyPrintJson")
    }
    return result
  }

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?) =
    """
      {
         "ApprovedPremises" : ${getCAS1Result(crn, nomsNumber, startDate, endDate)},
         "TemporaryAccommodation": ${getCAS3Result(crn, nomsNumber, startDate, endDate)},
         "ShortTermAccommodation": ${getCAS2Result(crn, nomsNumber, startDate, endDate)}
      }
    """.trimIndent()
}
