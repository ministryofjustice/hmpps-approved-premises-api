package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class SubjectAccessRequestServiceTest : IntegrationTestBase() {

  companion object {

    val START_DATE: LocalDateTime = LocalDateTime.of(2018, 9, 30, 0, 0, 0)
    val END_DATE: LocalDateTime = LocalDateTime.of(2024, 9, 30, 0, 0, 0)
    const val CREATED_AT = "2021-09-18T16:00:00+00:00"
    const val SUBMITTED_AT = "2021-10-19T16:00:00+00:00"
    const val SUBMITTED_AT_NO_TZ = "2021-10-19T16:00:00"
    const val ARRIVED_AT = "2021-09-20T16:00:00+00:00"
    const val ALLOCATED_AT = "2021-09-21T16:00:00+00:00"
    const val CREATED_AT_NO_TZ = "2021-09-18T16:00:00"
    const val DUE_AT = "2021-09-22T16:00:00+00:00"
    const val DEPARTED_AT = "2021-09-23T16:00:00+00:00"
    const val NEW_DEPARTED_AT = "2021-09-24T16:00:00+00:00"
    const val CANCELLATION_DATE = "2021-09-25T16:00:00+00:00"
    const val RESPONSE_RECEIVED_AT = "2021-10-23"
    const val APPEAL_DATE_ONLY = "2021-10-24"
    const val DECISION_MADE_AT = "2021-10-25T16:01:00+00:00"
    const val DECISION_MADE_AT_NO_TZ = "2021-10-25T16:01:00"

    var ARRIVED_AT_DATE_ONLY = this.ARRIVED_AT.substring(0..9)
    var DEPARTED_AT_DATE_ONLY = this.DEPARTED_AT.substring(0..9)
    var PREVIOUS_DEPARTURE_DATE_ONLY = this.DEPARTED_AT.substring(0..9)
    var NEW_DEPARTURE_DATE_ONLY = this.NEW_DEPARTED_AT.substring(0..9)
    var CANCELLATION_DATE_ONLY = this.CANCELLATION_DATE.substring(0..9)

    const val DATA_JSON_SIMPLE = """{ "key": "value" }"""
    const val DOCUMENT_JSON_SIMPLE = """{ "key2": "value2" }"""
    const val EVENT_NUMBER = "1"
    const val OFFENCE_ID = "BEING_BAD"
    const val CONVICTION_ID = 2L
    const val RELEASE_TYPE_CONDITIONAL = "CONDITIONAL"
    const val WITHDRAWAL_REASON_NOT_WITHDRAWN = "NOT WITHDRAWN"
    const val OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE = "NOT APPLICABLE"
    const val SENTENCE_TYPE_CUSTODIAL = "CUSTODIAL"
    const val NAME = "Jeffity Jeff"
  }

  @Autowired
  lateinit var sarService: SubjectAccessRequestService

  @Test
  fun `Get CAS1 Information - No Results`() {
    val (offenderDetails, _) = `Given an Offender`()
    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
     {
        "approvedPremises":
        {
          "Applications": [ ],
          "ApplicationTimeline": [ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
          }
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val (offenderDetails, _) = `Given an Offender`()

    val application = approvedPremisesApplicationEntity(offenderDetails)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises": 
      {    
        "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have application note`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)

    val timelineNotes = applicationTimelineNoteEntity(application)

    val result = sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {
        "Applications": ${approvedPremisesApplicationsJson(application, offender)},
        "ApplicationTimeline": ${approvedPremisesApplicationTimelineNotesJson(application, timelineNotes, offender)},
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
          
      }
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have assessment`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val assessment = approvedPremisesAssessment(application)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {  
        "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "ApplicationTimeline" :[ ],
        "Assessments": ${approvedPremisesAssessmentJson(application, offenderDetails, assessment)},
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
      }
    }
    """
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have assessment with clarification notes`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val assessment = approvedPremisesAssessment(application)
    val clarificationNote = approvedPremisesAssessmentClarificationNote(assessment)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offenderDetails, assessment)},
          "AssessmentClarificationNotes": ${
    approvedPremisesAssessmentClarificationNoteJson(
      assessment,
      offenderDetails,
      clarificationNote,
    )
    },
          "Bookings": [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking with extension`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val bookingExtension = bookingExtensionEntity(booking)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "BookingExtensions": ${bookingExtensionJson(bookingExtension)},
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking cancellation`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val cancellation = cancellationEntity(booking)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "BookingExtensions": [ ],
          "Cancellations": ${cancellationJson(cancellation)},
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a bed move`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val booking = bookingEntity(offender, application)

    val newBed = bedEntity()
    val bedMove = bedMoveEntity(booking, booking.bed!!, newBed)

    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": ${bedMovesJson(bedMove)},
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has an appeal`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessment(application)
    val appeal = appealEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessment(assessment)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withCreatedBy(application.createdByUser)
      withAppealDate(LocalDate.parse(APPEAL_DATE_ONLY))
      withAppealDetail("I want to appeal this decision")
      withDecision(AppealDecision.rejected)
      withDecisionDetail("rejected as no good")
    }

    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offender, assessment)},
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": ${appealsJson(appeal)},
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement application`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)

    val placementApplication = placementApplicationEntity(application)
    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": ${approvedPremisesPlacementApplicationsJson(placementApplication)},
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement request with requirements`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessment(application)
    val booking = bookingEntity(offender, application)
    val placementApplication = placementApplicationEntity(application)
    val allocatedUser = userEntity()

    val placementRequest = placementRequestEntity(booking, assessment, application, allocatedUser, placementApplication)
    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offender, assessment)},
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": ${approvedPremisesPlacementApplicationsJson(placementApplication)},
          "PlacementRequests": ${approvedPremisesPlacementRequestsJson(placementRequest)},
          "PlacementRequirements": ${placementRequirementJson(placementRequest.placementRequirements)},
          "PlacementRequirementCriteria" : ${placementRequirementCriteriaJson(placementRequest.placementRequirements)}
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  private fun placementRequirementCriteriaJson(placementRequirements: PlacementRequirementsEntity): String =
    """
      [
         {
            "crn": "${placementRequirements.application.crn}",
            "noms_number": "${placementRequirements.application.nomsNumber}",
            "placement_requirement_id": "${placementRequirements.id}",
            "criteria_name": "${placementRequirements.desirableCriteria[0].name}",
            "service_scope": "${placementRequirements.desirableCriteria[0].serviceScope}",
            "model_scope": "${placementRequirements.desirableCriteria[0].modelScope}",
            "property_name": "${placementRequirements.desirableCriteria[0].propertyName}",
            "is_active": ${placementRequirements.desirableCriteria[0].isActive},
            "criteria_type": "DESIRABLE"
        },
        {
            "crn": "${placementRequirements.application.crn}",
            "noms_number": "${placementRequirements.application.nomsNumber}",
            "placement_requirement_id": "${placementRequirements.id}",
            "criteria_name": "${placementRequirements.essentialCriteria[0].name}",
            "service_scope": "${placementRequirements.essentialCriteria[0].serviceScope}",
            "model_scope": "${placementRequirements.essentialCriteria[0].modelScope}",
            "property_name": "${placementRequirements.essentialCriteria[0].propertyName}",
            "is_active": ${placementRequirements.essentialCriteria[0].isActive},
            "criteria_type": "ESSENTIAL"
        }
      ]
    """.trimIndent()

  private fun placementRequirementJson(placementRequirement: PlacementRequirementsEntity): String =
    """
    [
        {
          "crn": "${placementRequirement.application.crn}",
          "noms_number": "${placementRequirement.application.nomsNumber}",
          "application_id": "${placementRequirement.application.id}",
          "assessment_id": "${placementRequirement.assessment.id}",
          "placement_requirements_id": "${placementRequirement.id}",
          "gender": "${placementRequirement.gender.value.uppercase()}",
          "ap_type": "${placementRequirement.apType.value.uppercase()}",
          "outcode": "${placementRequirement.postcodeDistrict.outcode}",
          "radius": ${placementRequirement.radius},
          "created_at": "$CREATED_AT"
        }
    ]
    """.trimIndent()

  private fun approvedPremisesPlacementRequestsJson(placementRequest: PlacementRequestEntity): String =
    """
    [
        {
          "crn": "${placementRequest.application.crn}",
          "noms_number": "${placementRequest.application.nomsNumber}",
          "expected_arrival": "$ARRIVED_AT_DATE_ONLY",
          "duration": ${placementRequest.duration},
          "created_at": "$CREATED_AT",
          "placement_application_id": "${placementRequest.placementApplication?.id}",
          "booking_id": "${placementRequest.booking?.id}",
          "application_id": "${placementRequest.application.id}",
          "assessment_id": "${placementRequest.assessment.id}",
          "notes": "${placementRequest.notes}",
          "is_parole": ${placementRequest.isParole},
          "is_withdrawn": ${placementRequest.isWithdrawn},
          "withdrawal_reason": "${placementRequest.withdrawalReason}",
          "due_at": "$DUE_AT"
        }
    ]
    """.trimIndent()

  private fun approvedPremisesPlacementApplicationsJson(placementApplication: PlacementApplicationEntity): String =
    """
    [
      {
        "crn": "${placementApplication.application.crn}",
        "noms_number": "${placementApplication.application.nomsNumber}",
        "application_id": "${placementApplication.application.id}",
        "data": $DATA_JSON_SIMPLE,
        "document": $DOCUMENT_JSON_SIMPLE,
        "created_at": "$CREATED_AT_NO_TZ",
        "submitted_at": "$SUBMITTED_AT_NO_TZ" ,
        "allocated_at": null,
        "reallocated_at": null,
        "due_at": null,
        "decision": "${placementApplication.decision}",
        "decision_made_at": "$DECISION_MADE_AT_NO_TZ" ,
        "placement_type": "${PlacementType.ADDITIONAL_PLACEMENT}",
        "is_withdrawn": ${placementApplication.isWithdrawn},
        "withdrawal_reason": "${placementApplication.withdrawalReason}",
        "created_by_user": "${placementApplication.createdByUser.name}",
        "allocated_user": null
      }
    ] 
    """.trimIndent()

  private fun appealsJson(appeal: AppealEntity): String =
    """
    [
      {
          "crn": "${appeal.application.crn}" ,
          "noms_number": "${appeal.application.nomsNumber}", 
          "appeal_id": "${appeal.id}",
          "application_id": "${appeal.application.id}",
          "assessment_id": "${appeal.assessment.id}",
          "appeal_date": "$APPEAL_DATE_ONLY",
          "appeal_detail": "${appeal.appealDetail}",
          "decision" : "${appeal.decision}",
          "decision_detail": "${appeal.decisionDetail}" ,
          "appeal_created_at": "$CREATED_AT" ,
          "created_by_user" :  "${appeal.createdBy.name}"
      }
    ]
    """.trimIndent()

  private fun bedMovesJson(bedMove: BedMoveEntity): String =
    """
    [
      {
        "crn": "${bedMove.booking.crn}" ,
        "noms_number": "${bedMove.booking.nomsNumber}",
        "notes": "${bedMove.notes}",
        "previous_bed_name": "${bedMove.previousBed!!.name}",
        "previous_bed_code":"${bedMove.previousBed!!.code}",
        "new_bed_name":"${bedMove.newBed.name}",
        "new_bed_code":"${bedMove.newBed.code}",
        "created_at": "$CREATED_AT_NO_TZ"
      }
    ]
    """.trimIndent()

  private fun cancellationJson(cancellation: CancellationEntity): String =
    """
    [
      {   
          "crn": "${cancellation.booking.crn}",
          "noms_number": "${cancellation.booking.nomsNumber}",
          "notes": "${cancellation.notes}",
          "cancellation_date": "$CANCELLATION_DATE_ONLY",
          "cancellation_reason": "${cancellation.reason.name}",
          "other_reason": "${cancellation.otherReason}",
          "created_at": "$CREATED_AT"
      }
    ]
    """.trimIndent()

  private fun bookingExtensionJson(bookingExtension: ExtensionEntity): String =
    """
    [
        {
          "application_id": "${bookingExtension.booking.application?.id}",
          "offline_application_id": ${bookingExtension.booking.offlineApplication?.let { "\"${bookingExtension.booking.offlineApplication!!.id}\"" }},
          "crn": "${bookingExtension.booking.crn}",
          "noms_number": "${bookingExtension.booking.nomsNumber}",
          "previous_departure_date": "$PREVIOUS_DEPARTURE_DATE_ONLY",
          "new_departure_date": "$NEW_DEPARTURE_DATE_ONLY",
          "notes": "${bookingExtension.notes}",
          "created_at": "$CREATED_AT"
        }
    ]
    """.trimIndent()

  private fun bookingsJson(booking: BookingEntity): String =
    """
    [
       {
          "crn": "${booking.crn}",
          "noms_number": "${booking.nomsNumber}",
          "arrival_date": "${booking.arrivalDate}",
          "departure_date": "${booking.departureDate}",
          "original_arrival_date": "${booking.originalArrivalDate}",
          "original_departure_date": "${booking.originalDepartureDate}",
          "created_at": "$CREATED_AT",
          "status": "${booking.status}",
          "premises_name": "${booking.premises.name}",
          "adhoc": ${booking.adhoc},
          "key_worker_staff_code": "${booking.keyWorkerStaffCode}",
          "service": "${booking.service}",
          "application_id": "${booking.application?.id}",
          "offline_application_id": ${if (booking.offlineApplication != null) "\"${booking.offlineApplication!!.id}\"" else "null"},
          "version": ${booking.version}
       }
    ]
    """.trimIndent()

  private fun approvedPremisesApplicationsJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
  ) = """
   [
       {
          "id": "${application.id}",
          "name": "$NAME",
          "crn": "${offenderDetails.otherIds.crn}",
          "noms_number": "${offenderDetails.otherIds.nomsNumber}",
          "data": $DATA_JSON_SIMPLE, 
          "document": $DOCUMENT_JSON_SIMPLE,
          "created_at": "$CREATED_AT",
          "submitted_at": "$SUBMITTED_AT",
          "created_by_user": "${application.createdByUser.name}",
          "application_user_name": "${application.applicantUserDetails?.name}",
          "event_number": "$EVENT_NUMBER",
          "is_womens_application": false,
          "offence_id": "$OFFENCE_ID",
          "conviction_id": $CONVICTION_ID,
          "risk_ratings": ${risksJson()}, 
          "release_type": "$RELEASE_TYPE_CONDITIONAL",
          "arrival_date": "$ARRIVED_AT",
          "is_withdrawn": false,
          "withdrawal_reason": "$WITHDRAWAL_REASON_NOT_WITHDRAWN",
          "other_withdrawal_reason": "$OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE",
          "is_emergency_application": true,
          "target_location": null,
          "status": "${ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT}",
          "inmate_in_out_status_on_submission": null,
          "sentence_type": "$SENTENCE_TYPE_CUSTODIAL",
          "notice_type":  "${Cas1ApplicationTimelinessCategory.emergency}",
          "ap_type": "${ApprovedPremisesType.NORMAL}",
          "case_manager_name": "${application.caseManagerUserDetails?.name}",
          "case_manager_is_not_applicant" : true
       }
   ]
  """.trimIndent()

  private fun approvedPremisesApplicationTimelineNotesJson(
    application: ApprovedPremisesApplicationEntity,
    timelineNote: ApplicationTimelineNoteEntity,
    offender: OffenderDetailSummary,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): String =
    """
    [ 
      {
          "application_id":"${application.id}",
          "service":"${serviceName.value}",
          "crn":"${offender.otherIds.crn}",
          "noms_number":"${offender.otherIds.nomsNumber}",
          "body":"${timelineNote.body}",
          "created_at":"$CREATED_AT_NO_TZ",
          "user_name":"${timelineNote.createdBy?.name}"
      }
    ]
    """.trimIndent()

  private fun approvedPremisesAssessmentJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
    assessment: ApprovedPremisesAssessmentEntity,
  ): String =
    """
    [
       {
          "application_id":"${application.id}",
          "assessment_id":"${assessment.id}",
          "crn":"${offenderDetails.otherIds.crn}",
          "noms_number":"${offenderDetails.otherIds.nomsNumber}",
          "assessor_name":"${assessment.allocatedToUser?.name}",
          "data":$DATA_JSON_SIMPLE,
          "document":$DOCUMENT_JSON_SIMPLE,
          "created_at":"$CREATED_AT",
          "allocated_at":"$ALLOCATED_AT",
          "submitted_at":"$SUBMITTED_AT",
          "reallocated_at":null,
          "due_at":"$DUE_AT",
          "decision":"${AssessmentDecision.REJECTED}",
          "rejection_rationale":"rejected as no good",
          "service":"approved-premises",
          "is_withdrawn":false,
          "created_from_appeal":false
       }
 ]
    """.trimIndent()

  private fun risksJson(): String =
    """
    {
        "roshRisks" : {
          "status" : "NotFound",
          "value" : null
        },
        "mappa" : {
          "status" : "NotFound",
          "value" : null
        },
        "tier" : {
          "status" : "Retrieved",
          "value" : {
            "level" : "M1",
            "lastUpdated" : [ 2023, 6, 26 ]
          }
        },
        "flags" : {
          "status" : "NotFound",
          "value" : null
        }
    }
    """.trimIndent()

  private fun approvedPremisesAssessmentClarificationNoteJson(
    assessment: ApprovedPremisesAssessmentEntity,
    offenderDetails: OffenderDetailSummary,
    clarificationNote: AssessmentClarificationNoteEntity,
  ) = """
      [
        {
          "application_id": "${assessment.application.id}",
          "assessment_id": "${assessment.id}",
          "crn": "${offenderDetails.otherIds.crn}",
          "noms_number": "${offenderDetails.otherIds.nomsNumber}",
          "created_at": "$CREATED_AT",
          "query": "${clarificationNote.query}",
          "response": "${clarificationNote.response}",
          "created_by_user": "${clarificationNote.createdByUser.name}",   
        }
      ]
  """.trimIndent()

  private fun placementRequestEntity(
    booking: BookingEntity,
    assessment: ApprovedPremisesAssessmentEntity,
    application: ApprovedPremisesApplicationEntity,
    allocatedUser: UserEntity,
    placementApplication: PlacementApplicationEntity,
  ) = placementRequestFactory.produceAndPersist {
    withBooking(booking)
    withAssessment(assessment)
    withApplication(application)
    withPlacementApplication(placementApplication)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedToUser(allocatedUser)
    withDueAt(OffsetDateTime.parse(DUE_AT))
    withDuration(5)
    withExpectedArrival(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
    withIsParole(false)
    withIsWithdrawn(true)
    withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
    withNotes("some notes")
    withPlacementRequirements(placementRequirementEntity(application, assessment))
  }

  private fun placementRequirementEntity(
    application: ApprovedPremisesApplicationEntity,
    assessment: ApprovedPremisesAssessmentEntity,
  ) = placementRequirementsFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withApType(ApType.normal)
    withDesirableCriteria(listOf(characteristic()))
    withEssentialCriteria(listOf(characteristic()))
    withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
  }

  private fun characteristic() =
    characteristicEntityFactory.produceAndPersist {
      withName(randomStringMultiCaseWithNumbers(10))
      withServiceScope(Characteristic.ServiceScope.star.value)
      withModelScope(Characteristic.ModelScope.room.value)
      withPropertyName(randomStringMultiCaseWithNumbers(6))
    }
  private fun placementApplicationEntity(application: ApprovedPremisesApplicationEntity) =
    placementApplicationFactory.produceAndPersist {
      withApplication(application)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withDueAt(null)
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
      withAllocatedToUser(null)
      withCreatedByUser(application.createdByUser)
      withDecision(PlacementApplicationDecision.ACCEPTED)
      withDecisionMadeAt(OffsetDateTime.parse(DECISION_MADE_AT))
      withIsWithdrawn(true)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      withReallocatedAt(null)
      withSchemaVersion(approvedPremisesPlacementApplicationJsonSchemaEntity())
      withWithdrawalReason(PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
    }

  private fun approvedPremisesApplicationJsonSchemaEntity(): ApprovedPremisesApplicationJsonSchemaEntity =
    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun approvedPremisesAssessmentJsonSchemaEntity(): ApprovedPremisesAssessmentJsonSchemaEntity =
    approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun approvedPremisesPlacementApplicationJsonSchemaEntity() =
    approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun cas1ApplicationUserDetailsEntity(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname_applicant_user@noname.net")
    }

  private fun cas1CaseManagerUserDetailsEntity(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname@noname.net")
    }

  private fun applicationTimelineNoteEntity(application: ApprovedPremisesApplicationEntity) =
    applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(application.id)
      withBody("Some random note about this application")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withCreatedBy(application.createdByUser)
    }

  private fun bedMoveEntity(booking: BookingEntity, previousBed: BedEntity, newBed: BedEntity): BedMoveEntity =
    this.bedMoveEntityFactory.produceAndPersist {
      withBooking(booking)
      withPreviousBed(previousBed)
      withNewBed(newBed)
      withNotes("Some Notes about a bed move")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }

  private fun cancellationEntity(booking: BookingEntity): CancellationEntity =
    cancellationEntityFactory.produceAndPersist {
      withReason(
        cancellationReasonEntityFactory.produceAndPersist {
          withName("some reason")
          withServiceScope("approved-premises")
          withIsActive(true)
        },
      )
      withBooking(booking)
      withNotes("some notes")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withDate(LocalDate.parse(CANCELLATION_DATE_ONLY))
      withOtherReason("some other reason")
    }

  private fun bookingExtensionEntity(booking: BookingEntity): ExtensionEntity {
    return extensionEntityFactory.produceAndPersist {
      withBooking(booking)
      withNotes("some notes")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withPreviousDepartureDate(LocalDate.parse(PREVIOUS_DEPARTURE_DATE_ONLY))
      withNewDepartureDate(LocalDate.parse(NEW_DEPARTURE_DATE_ONLY))
    }
  }

  private fun bookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApprovedPremisesApplicationEntity,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): BookingEntity {
    val bed = bedEntity()

    val booking = bookingEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withAdhoc(true)
      withDepartureDate(LocalDate.parse(DEPARTED_AT_DATE_ONLY))
      withApplication(application)
      withArrivalDate(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
      withOriginalArrivalDate(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
      withOriginalDepartureDate(LocalDate.parse(DEPARTED_AT_DATE_ONLY))
      withPremises(bed.room.premises)
      withStaffKeyWorkerCode("KEYWORKERSTAFFCODE")
      withStatus(BookingStatus.arrived)
      withBed(bed)
      withServiceName(serviceName)
    }
    return booking
  }

  private fun bedEntity() = bedEntityFactory.produceAndPersist {
    withName("a bed ${randomStringMultiCaseWithNumbers(5)}")
    withCode("a code ${randomStringMultiCaseWithNumbers(5)}")
    withRoom(
      roomEntityFactory.produceAndPersist {
        withCode("room code ${randomStringMultiCaseWithNumbers(5)}")
        withName("room name ${randomStringMultiCaseWithNumbers(5)}")

        withPremises(
          approvedPremisesEntityFactory.produceAndPersist {
            withName("a premises ${randomStringMultiCaseWithNumbers(5)}")
            withApCode("AP Code ${randomStringMultiCaseWithNumbers(5)}")
            withLocalAuthorityArea(
              localAuthorityEntityFactory.produceAndPersist {
                withName("An LAA ${randomStringMultiCaseWithNumbers(5)}")
                withIdentifier("LAA ID ${randomStringMultiCaseWithNumbers(5)}")
              },
            )
            withProbationRegion(
              probationRegionEntityFactory.produceAndPersist {
                withName("Probation Region ${randomStringMultiCaseWithNumbers(5)}")
                withApArea(
                  apAreaEntityFactory.produceAndPersist {
                    withName("Probation Area ${randomStringMultiCaseWithNumbers(5)}")
                  },
                )
              },
            )
          },
        )
      },
    )
  }

  private fun userEntity(): UserEntity =
    userEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(
            apAreaEntityFactory.produceAndPersist {
            },
          )
        },
      )
    }

  private fun personRisks(): PersonRisks =
    PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      ).produce()

  private fun approvedPremisesApplicationEntity(offenderDetails: OffenderDetailSummary): ApprovedPremisesApplicationEntity {
    val user = userEntity()
    val risk1 = personRisks()
    val applicantUserDetails = cas1ApplicationUserDetailsEntity()
    val caseManagerUserDetails = cas1CaseManagerUserDetailsEntity()
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntity()
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withName(NAME)
      withCreatedByUser(user)
      withEventNumber(EVENT_NUMBER)
      withIsWomensApplication(false)
      withOffenceId(OFFENCE_ID)
      withConvictionId(CONVICTION_ID)
      withRiskRatings(risk1)
      withReleaseType(RELEASE_TYPE_CONDITIONAL)
      withArrivalDate(OffsetDateTime.parse(ARRIVED_AT))
      withIsWithdrawn(false)
      withWithdrawalReason(WITHDRAWAL_REASON_NOT_WITHDRAWN)
      withOtherWithdrawalReason(OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE)
      withIsEmergencyApplication(true)
      withTargetLocation(null)
      withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
      withInmateInOutStatusOnSubmission(null)
      withSentenceType(SENTENCE_TYPE_CUSTODIAL)
      withNoticeType(Cas1ApplicationTimelinessCategory.emergency)
      withApType(ApprovedPremisesType.NORMAL)
      withApplicantUserDetails(applicantUserDetails)
      withCaseManagerUserDetails(caseManagerUserDetails)
      withCaseManagerIsNotApplicant(true)
      withApplicationSchema(applicationSchema)
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
    }
    return application
  }

  private fun approvedPremisesAssessmentClarificationNote(assessment: ApprovedPremisesAssessmentEntity): AssessmentClarificationNoteEntity =
    assessmentClarificationNoteEntityFactory.produceAndPersist() {
      withAssessment(assessment)
      withCreatedBy(assessment.allocatedToUser!!)
      withQuery("some query")
      withResponse("a useful response")
      withResponseReceivedOn(LocalDate.parse(RESPONSE_RECEIVED_AT))
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }

  private fun approvedPremisesAssessment(
    application: ApprovedPremisesApplicationEntity,
  ): ApprovedPremisesAssessmentEntity = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    withData(DATA_JSON_SIMPLE)
    withDocument(DOCUMENT_JSON_SIMPLE)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedAt(OffsetDateTime.parse(ALLOCATED_AT))
    withIsWithdrawn(false)
    withAllocatedToUser(userEntity())
    withApplication(application)
    withAssessmentSchema(approvedPremisesAssessmentJsonSchemaEntity())
    withCreatedFromAppeal(false)
    withDecision(AssessmentDecision.REJECTED)
    withReallocatedAt(null)
    withRejectionRationale("rejected as no good")
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withDueAt(OffsetDateTime.parse(DUE_AT))
  }
}
