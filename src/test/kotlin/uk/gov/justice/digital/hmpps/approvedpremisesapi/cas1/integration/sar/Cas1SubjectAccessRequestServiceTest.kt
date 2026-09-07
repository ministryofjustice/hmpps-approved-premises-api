package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.service.Cas1SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

@SuppressWarnings("LargeClass")
class Cas1SubjectAccessRequestServiceTest : Cas1SarTestBase() {

  @Autowired
  lateinit var cas1SarService: Cas1SubjectAccessRequestService

  @Test
  fun `Get CAS1 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      cas1SarService.getSarResult(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    assertNull(result)
  }

  @Test
  fun `Get CAS1 Information - Test Null Dates`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      cas1SarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)

    assertNull(result)
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val (offenderDetails, _) = givenAnOffender()

    val application = approvedPremisesApplicationEntity(offenderDetails)

    val result =
      cas1SarService.getSarResult(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )
    assertNotNull(result)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[],
        "Assessments": [],
        "AssessmentClarificationNotes": [],
        "SpaceBookings": [],
        "Appeals": [],
        "PlacementApplications": [],
        "PlacementRequests": [],
        "PlacementRequirements": [],
        "PlacementRequirementCriteria" : [],
        "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have application note`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)

    val timelineNotes = applicationTimelineNoteEntity(application)

    val result = cas1SarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline": [${approvedPremisesApplicationTimelineNotesJson(timelineNotes)}],
        "Assessments": [],
        "AssessmentClarificationNotes": [],
        "SpaceBookings": [],
        "Appeals": [],
        "PlacementApplications": [],
        "PlacementRequests": [],
        "PlacementRequirements": [],
        "PlacementRequirementCriteria" : [],
        "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have assessment`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val assessment = approvedPremisesAssessmentEntity(application)

    val result =
      cas1SarService.getSarResult(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    assertNotNull(result)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[],
        "Assessments": [${approvedPremisesAssessmentJson(assessment)}],
        "AssessmentClarificationNotes": [],
        "SpaceBookings": [],
        "Appeals": [],
        "PlacementApplications": [],
        "PlacementRequests": [],
        "PlacementRequirements": [],
        "PlacementRequirementCriteria" : [],
        "BookingNotMades" : []
    }
    """
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have assessment with clarification notes`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val assessment = approvedPremisesAssessmentEntity(application)
    val clarificationNote = approvedPremisesAssessmentClarificationNoteEntity(assessment)

    val result =
      cas1SarService.getSarResult(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    assertNotNull(result)

    val expectedJson = """
    {
       "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
       "ApplicationTimeline" :[],
       "Assessments": [${approvedPremisesAssessmentJson(assessment)}],
       "AssessmentClarificationNotes": [${
      approvedPremisesAssessmentClarificationNoteJson(
        clarificationNote,
      )
    }],
       "SpaceBookings": [],
       "Appeals": [],
       "PlacementApplications": [],
       "PlacementRequests": [],
       "PlacementRequirements": [],
       "PlacementRequirementCriteria" : [],
       "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a space booking`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()
    val departureReason = departureReasonEntityFactory.produceAndPersist()
    val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist()
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()
    val booking = spaceBookingEntity(
      offenderDetails = offenderDetails,
      application = application,
      nonArrivalReason = nonArrivalReason,
      departureReason = departureReason,
      moveOnCategory = moveOnCategory,
      cancellationReason = cancellationReason,
      additionalInformation = ADDITIONAL_INFORMATION,
      transferReason = TRANSFER_REASON,
    )

    val result =
      cas1SarService.getSarResult(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    assertNotNull(result)

    val expectedJson = """
    {        
      "Applications":[${approvedPremisesApplicationsJson(application, offenderDetails)}],
      "ApplicationTimeline" :[],
      "Assessments": [],
      "AssessmentClarificationNotes": [],
      "SpaceBookings":  [ ${spaceBookingsJson(booking)} ],
      "Appeals": [],
      "PlacementApplications": [],
      "PlacementRequests": [],
      "PlacementRequirements": [],
      "PlacementRequirementCriteria" : [],
      "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has an appeal`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val appeal = appealEntity(application, assessment)

    val result =
      cas1SarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {        
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[],
        "Assessments": [${approvedPremisesAssessmentJson(assessment)}],
        "AssessmentClarificationNotes": [],
        "SpaceBookings": [],
        "Appeals":[ ${appealsJson(appeal)}],
        "PlacementApplications": [],
        "PlacementRequests": [],
        "PlacementRequirements": [],
        "PlacementRequirementCriteria" : [],
        "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement application`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)

    val placementApplication = placementApplicationEntity(application)
    val result =
      cas1SarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {
      "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
      "ApplicationTimeline" :[],
      "Assessments": [],
      "AssessmentClarificationNotes": [],
      "SpaceBookings": [],
      "Appeals": [],
      "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
      "PlacementRequests": [],
      "PlacementRequirements": [],
      "PlacementRequirementCriteria" : [],
      "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement request with requirements`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val placementApplication = placementApplicationEntity(application)

    val placementRequest = placementRequestEntity(assessment, application, placementApplication)
    val result =
      cas1SarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {   
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[],
        "Assessments": [${approvedPremisesAssessmentJson(assessment)}],
        "AssessmentClarificationNotes": [],
        "SpaceBookings":  [],    
        "Appeals": [],
        "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
        "PlacementRequests": [${approvedPremisesPlacementRequestsJson(placementRequest)}],
        "PlacementRequirements": [${placementRequirementJson(placementRequest.placementRequirements)}],
        "PlacementRequirementCriteria" : [${placementRequirementCriteriaJson(placementRequest.placementRequirements)}],
        "BookingNotMades" : []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has bookings not made`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val placementApplication = placementApplicationEntity(application)
    val placementRequest = placementRequestEntity(assessment, application, placementApplication)
    val bookingNotMade = bookingNotMadeEntity(placementRequest)

    val result =
      cas1SarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {
      "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
      "ApplicationTimeline" :[],
      "Assessments": [${approvedPremisesAssessmentJson(assessment)}],
      "AssessmentClarificationNotes": [],
      "SpaceBookings":  [],     
      "Appeals": [],
      "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
      "PlacementRequests": [${approvedPremisesPlacementRequestsJson(placementRequest)}],
      "PlacementRequirements": [${placementRequirementJson(placementRequest.placementRequirements)}],
      "PlacementRequirementCriteria" : [${placementRequirementCriteriaJson(placementRequest.placementRequirements)}],
      "BookingNotMades": [${bookingsNotMadeJson(bookingNotMade)}]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  private fun placementRequirementCriteriaJson(placementRequirements: PlacementRequirementsEntity): String =
    """
      {
          "criteria_name": "${placementRequirements.desirableCriteria[0].name}",
          "property_name": "${placementRequirements.desirableCriteria[0].propertyName}",
          "criteria_type": "DESIRABLE"
      },
      {
          "criteria_name": "${placementRequirements.essentialCriteria[0].name}",
          "property_name": "${placementRequirements.essentialCriteria[0].propertyName}",
          "criteria_type": "ESSENTIAL"
      }
    """.trimIndent()

  private fun placementRequirementJson(placementRequirement: PlacementRequirementsEntity): String =
    """
      {
        "ap_type": "${placementRequirement.apType.name}",
        "outcode": "${placementRequirement.postcodeDistrict.outcode}",
        "radius": ${placementRequirement.radius},
        "created_at": "$CREATED_AT"
      }
    """.trimIndent()

  private fun approvedPremisesPlacementRequestsJson(placementRequest: PlacementRequestEntity): String =
    """
      {
        "expected_arrival": "$arrivedAtDateOnly",
        "duration": ${placementRequest.duration},
        "created_at": "$CREATED_AT",
        "notes": "${placementRequest.notes}",
        "is_parole": ${placementRequest.isParole},
        "is_withdrawn": ${placementRequest.isWithdrawn},
        "withdrawal_reason": "${placementRequest.withdrawalReason}"
      }
    """.trimIndent()

  private fun approvedPremisesPlacementApplicationsJson(placementApplication: PlacementApplicationEntity): String =
    """
      {
        "document": $DOCUMENT_JSON_SIMPLE,
        "created_at": "$CREATED_AT_NO_TZ",
        "submitted_at": "$SUBMITTED_AT_NO_TZ" ,
        "allocated_at": "$ALLOCATED_AT_NO_TZ" ,
        "reallocated_at": "$REALLOCATED_AT_NO_TZ",
        "due_at": "$DUE_AT" ,
        "decision": "${placementApplication.decision}",
        "decision_made_at": "$DECISION_MADE_AT_NO_TZ" ,
        "placement_type": "${PlacementType.ADDITIONAL_PLACEMENT}",
        "is_withdrawn": ${placementApplication.isWithdrawn},
        "withdrawal_reason": "${placementApplication.withdrawalReason}",
        "created_by_user": "${placementApplication.createdByUser.name}",
        "allocated_user": "${placementApplication.allocatedToUser?.name}",
        "sentence_type": "$SENTENCE_TYPE_CUSTODIAL",
        "release_type": "$RELEASE_TYPE_CONDITIONAL",
        "requested_duration": $REQUESTED_DURATION,
        "authorised_duration": $AUTHORISED_DURATION,
        "expected_arrival": "$arrivedAtDateOnly",
        "expected_arrival_flexible": true,
        "situation": "${SituationOption.awaitingSentence}"
      }
    """.trimIndent()

  private fun appealsJson(appeal: AppealEntity): String =
    """
      {
          "appeal_date": "$APPEAL_DATE_ONLY",
          "appeal_detail": "${appeal.appealDetail}",
          "decision" : "${appeal.decision}",
          "decision_detail": "${appeal.decisionDetail}" ,
          "appeal_created_at": "$CREATED_AT" ,
          "created_by_user" :  "${appeal.createdBy.name}"
      }
    """.trimIndent()

  private fun approvedPremisesApplicationsJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
  ): String = """
        {
           "name": "$NAME",
           "noms_number": "${offenderDetails.otherIds.nomsNumber}",
           "document": $DOCUMENT_JSON_SIMPLE,
           "created_at": "$CREATED_AT",
           "submitted_at": "$SUBMITTED_AT",
           "application_user_name": "${application.createdByUser.deliusUsername}",
           "event_number": "$EVENT_NUMBER",
           "is_womens_application": false,
           "risk_ratings": ${risksJson()}, 
           "release_type": "$RELEASE_TYPE_CONDITIONAL",
           "arrival_date": "$ARRIVED_AT",
           "is_withdrawn": false,
           "withdrawal_reason": "$WITHDRAWAL_REASON_NOT_WITHDRAWN",
           "other_withdrawal_reason": "$OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE",
           "is_emergency_application": true,
           "target_location": null,
           "status": "${ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT}",
           "inmate_in_out_status_on_submission": "OUT",
           "sentence_type": "$SENTENCE_TYPE_CUSTODIAL",
           "notice_type":  "${Cas1ApplicationTimelinessCategory.emergency}",
           "ap_type": "${ApprovedPremisesType.NORMAL}",
           "case_manager_name": ${application.caseManagerUserDetails?.name?.let { "\"${it.trim().substringAfterLast(' ')}\"" } ?: "null"},
           "case_manager_is_not_applicant" : true,
           "situation": "${SituationOption.bailSentence}",
           "is_inapplicable": false,
           "licence_expiry_date": "$LICENCE_EXPIRY_DATE",
           "expired_reason": "$EXPIRED_REASON"
        }
  """.trimIndent()

  private fun approvedPremisesApplicationTimelineNotesJson(
    timelineNote: ApplicationTimelineNoteEntity,
  ): String =
    """
      {
          "body":"${timelineNote.body}",
          "created_at":"$CREATED_AT_NO_TZ",
          "user_name":"${timelineNote.createdBy?.name}"
      }
    """.trimIndent()

  private fun approvedPremisesAssessmentJson(
    assessment: ApprovedPremisesAssessmentEntity,
  ): String =
    """
      {
         "assessor_name":"${assessment.allocatedToUser?.deliusUsername}",
         "document":$DOCUMENT_JSON_SIMPLE,
         "created_at":"$CREATED_AT",
         "allocated_at":"$ALLOCATED_AT",
         "submitted_at":"$SUBMITTED_AT",
         "reallocated_at":null,
         "due_at":"$DUE_AT",
         "decision":"${AssessmentDecision.REJECTED}",
         "rejection_rationale":"rejected as no good",
         "is_withdrawn":false,
         "created_from_appeal":false,
         "agree_with_short_notice_reason": false,
         "agree_with_short_notice_reason_comments": "$REASON_COMMENTS",
         "reason_for_late_application": "$LATE_APPLICATION_REASON"
      }
    """.trimIndent()

  private fun approvedPremisesAssessmentClarificationNoteJson(
    clarificationNote: AssessmentClarificationNoteEntity,
  ): String =
    """
      {
        "created_at": "$CREATED_AT",
        "query": "${clarificationNote.query}",
        "response": "${clarificationNote.response}",
        "response_received_on": "$RESPONSE_RECEIVED_AT",
        "created_by_user": "${clarificationNote.createdByUser.name}"
      }
    """.trimIndent()

  private fun bookingsNotMadeJson(bookingNotMade: BookingNotMadeEntity): String =
    """
      {
        "created_at": "$CREATED_AT_NO_TZ",
        "notes": "${bookingNotMade.notes}"
      }
    """.trimIndent()
}
