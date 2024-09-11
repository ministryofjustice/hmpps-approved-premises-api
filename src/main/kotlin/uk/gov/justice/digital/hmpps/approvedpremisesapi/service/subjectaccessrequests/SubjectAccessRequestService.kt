package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests

import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS1SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS2SubjectAccessRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CAS3SubjectAccessRequestRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class SubjectAccessRequestService(
  val objectMapper: ObjectMapper,
  val cas1SubjectAccessRequestRepository: CAS1SubjectAccessRequestRepository,
  val cas2SubjectAccessRequestRepository: CAS2SubjectAccessRequestRepository,
  val cas3SubjectAccessRequestRepository: CAS3SubjectAccessRequestRepository,
) : HmppsPrisonProbationSubjectAccessRequestService {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCAS1Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val approvedPremisesApplicationsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val apApplicationTimelineJson = cas1SubjectAccessRequestRepository.getApprovedPremisesApplicationTimeLineJson(crn, nomsNumber, startDate, endDate)
    val apAssessmentsJson = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessments(crn, nomsNumber, startDate, endDate)
    val apAssessmentClarificationNotesJson = cas1SubjectAccessRequestRepository.getApprovedPremisesAssessmentClarificationNotes(crn, nomsNumber, startDate, endDate)

    val apBookingsJson = cas1SubjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate)
    val apBookingExtensionsJson = cas1SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate)
    val apCancellationsJson = cas1SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate)
    val apBedMovesJson = cas1SubjectAccessRequestRepository.bedMoves(crn, nomsNumber, startDate, endDate)
    val domainEventsJson = cas1SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate)
    val domainEventMetaDataJson = cas1SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate)

    val placementApplicationsJson = cas1SubjectAccessRequestRepository.placementApplications(crn, nomsNumber, startDate, endDate)
    val placementRequestsJson = cas1SubjectAccessRequestRepository.placementRequests(crn, nomsNumber, startDate, endDate)
    val placementRequirementsJson = cas1SubjectAccessRequestRepository.placementRequirements(crn, nomsNumber, startDate, endDate)
    val placementRequirementCriteriaJson = cas1SubjectAccessRequestRepository.placementRequirementsCriteria(crn, nomsNumber, startDate, endDate)
    val offlineApplicationsJson = cas1SubjectAccessRequestRepository.offlineApplications(crn, nomsNumber, startDate, endDate)
    val bookingNotMadesJson = cas1SubjectAccessRequestRepository.bookingNotMades(crn, nomsNumber, startDate, endDate)
    val appealsJson = cas1SubjectAccessRequestRepository.appeals(crn, nomsNumber, startDate, endDate)

    val result = """
      {
         "Applications": $approvedPremisesApplicationsJson,
         "ApplicationTimeline": $apApplicationTimelineJson,
         "Assessments": $apAssessmentsJson,
         "AssessmentClarificationNotes" : $apAssessmentClarificationNotesJson,
         "Bookings": $apBookingsJson,
         "OfflineApplications": $offlineApplicationsJson,
         "BookingExtensions": $apBookingExtensionsJson,
         "Cancellations": $apCancellationsJson,
         "BedMoves": $apBedMovesJson,
         "Appeals": $appealsJson,
         "PlacementApplications": $placementApplicationsJson,
         "PlacementRequests": $placementRequestsJson,
         "PlacementRequirements": $placementRequirementsJson,
         "PlacementRequirementCriteria": $placementRequirementCriteriaJson,
         "BookingNotMades": $bookingNotMadesJson,
         "DomainEvents": $domainEventsJson,
         "DomainEventsMetadata": $domainEventMetaDataJson
      }
    """.trimIndent()
    log.logDebugMessage("CAS1", result)

    return result
  }

  fun getCAS3Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val temporaryAccommodationApplicationsJson = cas3SubjectAccessRequestRepository.temporaryAccommodationApplications(crn, nomsNumber, startDate, endDate)
    val temporaryAccommodationAssessmentsJson = cas3SubjectAccessRequestRepository.temporaryAccommodationAssessments(crn, nomsNumber, startDate, endDate)
    val assessmentReferralHistoryNotesJson = cas3SubjectAccessRequestRepository.assessmentReferralHistoryNotes(crn, nomsNumber, startDate, endDate)
    val bookingsJson = cas3SubjectAccessRequestRepository.bookings(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val bookingExtensionsJson = cas3SubjectAccessRequestRepository.bookingExtensions(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val cancellationsJson = cas3SubjectAccessRequestRepository.cancellations(crn, nomsNumber, startDate, endDate, ServiceName.temporaryAccommodation)
    val domainEventsJson = cas3SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS3")
    val domainEventsMetaDataJson = cas3SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate, "CAS3")

    val result = """
      {
        "Applications": $temporaryAccommodationApplicationsJson,
        "Assessments": $temporaryAccommodationAssessmentsJson,
        "AssessmentReferralHistoryNotes": $assessmentReferralHistoryNotesJson,
        "Bookings": $bookingsJson,
        "BookingExtensions": $bookingExtensionsJson,
        "Cancellations": $cancellationsJson,
        "DomainEvents": $domainEventsJson,
        "DomainEventsMetadata": $domainEventsMetaDataJson
      }
    """.trimIndent()

    log.logDebugMessage("CAS3", result)
    return result
  }

  fun getCAS2Result(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val applicationsJson = cas2SubjectAccessRequestRepository.getApplicationsJson(crn, nomsNumber, startDate, endDate)
    val applicationNotesJson =
      cas2SubjectAccessRequestRepository.getApplicationNotes(crn, nomsNumber, startDate, endDate)
    val statusUpdatesJson = cas2SubjectAccessRequestRepository.getStatusUpdates(crn, nomsNumber, startDate, endDate)
    val statusUpdateDetailsJson =
      cas2SubjectAccessRequestRepository.getStatusUpdateDetails(crn, nomsNumber, startDate, endDate)
    val assessmentsJson = cas2SubjectAccessRequestRepository.getAssessments(crn, nomsNumber, startDate, endDate)
    val domainEventsJson = cas2SubjectAccessRequestRepository.domainEvents(crn, nomsNumber, startDate, endDate, "CAS2")
    val domainEventsMetaDataJson =
      cas2SubjectAccessRequestRepository.domainEventMetadata(crn, nomsNumber, startDate, endDate, "CAS2")

    val result = """
     {
        "Applications": $applicationsJson,
        "ApplicationNotes": $applicationNotesJson,
        "Assessments": $assessmentsJson,
        "StatusUpdates": $statusUpdatesJson,
        "StatusUpdateDetails": $statusUpdateDetailsJson,
        "DomainEvents": $domainEventsJson,
        "DomainEventsMetadata": $domainEventsMetaDataJson
     }
    """.trimIndent()
    log.logDebugMessage("CAS2", result)

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

  override fun getContentFor(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? =
    HmppsSubjectAccessRequestContent(
      content = JSONObject(
        getSarResult(
          crn,
          prn,
          fromDate?.atStartOfDay(),
          toDate?.atTime(LocalTime.MAX),
        ),
      ).toMap().entries,
    )
  private fun Logger.logDebugMessage(service: String, result: String) {
    if (this.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("$service SAR Result is $prettyPrintJson")
    }
  }
}
