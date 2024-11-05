package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

class CAS3SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS3 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
     {
         "Applications": [ ],
         "Assessments": [ ],
         "AssessmentReferralHistoryNotes" : [ ],
         "Bookings": [ ],
         "BookingExtensions": [ ],
         "Cancellations": [ ],
         "DomainEvents": [ ],
         "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS3 Information - Null dates check`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)
    assertJsonEquals(
      """ 
     {
         "Applications": [ ],
         "Assessments": [ ],
         "AssessmentReferralHistoryNotes" : [ ],
         "Bookings": [ ],
         "BookingExtensions": [ ],
         "Cancellations": [ ],
         "DomainEvents": [ ],
         "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS3 Information - Applications`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments"  : [ ],
        "AssessmentReferralHistoryNotes" : [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 Information - Assessments`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val temporaryAccomodationAssessment = temporaryAccommodationAssessmentEntity(temporaryAccommodationApplication)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments"  : [${temporaryAccommodationAssessmentJson(temporaryAccomodationAssessment)}],
        "AssessmentReferralHistoryNotes" : [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 Information - Assessment Referral History Notes`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val temporaryAccomodationAssessment = temporaryAccommodationAssessmentEntity(temporaryAccommodationApplication)
    val assessmentReferralHistoryNoteSystem =
      assessmentReferralHistorySystemNoteEntity(temporaryAccomodationAssessment, user)
    val assessmentReferralHistoryNoteUser = assessmentReferralHistoryUserNoteEntity(temporaryAccomodationAssessment, user)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications": [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments": [${temporaryAccommodationAssessmentJson(temporaryAccomodationAssessment)}],
        "AssessmentReferralHistoryNotes": [${assessmentReferralHistoryNotesJson(assessmentReferralHistoryNoteSystem, assessmentReferralHistoryNoteUser)} ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)

    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {        
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [ ],
        "AssessmentReferralHistoryNotes" : [ ],
        "Bookings": [${bookingsJson(booking)}],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking with extension`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)

    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)
    val bookingExtension = bookingExtensionEntity(booking)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [ ],
        "AssessmentReferralHistoryNotes" : [ ],
        "Bookings": [${bookingsJson(booking)}],
        "BookingExtensions": [${bookingExtensionJson(bookingExtension)}],
        "Cancellations": [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking cancellation`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)
    val cancellation = cancellationEntity(booking)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {      
      "Applications" : [${temporaryAccommodationApplicationJson(application)}],
      "Assessments"  : [ ],
      "AssessmentReferralHistoryNotes" : [ ], 
      "Bookings": [${bookingsJson(booking)}],
      "BookingExtensions": [ ],
      "Cancellations": [${cancellationJson(cancellation)}],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS3 information - Domain Events`() {
    val (offender, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offender, user)
    val assessment = temporaryAccommodationAssessmentEntity(application)

    val domainEvent = domainEventEntity(offender, application.id, assessment.id, user.id, ServiceName.temporaryAccommodation)
    val result = sarService.getCAS3Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [${temporaryAccommodationAssessmentJson(assessment)}],
        "AssessmentReferralHistoryNotes" : [ ],
        "Bookings": [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "DomainEvents": [${domainEventJson(domainEvent,user)}],
        "DomainEventsMetadata": [${domainEventsMetadataJson(domainEvent)}]
      }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  private fun assessmentReferralHistoryNotesJson(
    assessmentReferralHistoryNoteSystem: AssessmentReferralHistorySystemNoteEntity,
    assessmentReferralHistoryNoteUser: AssessmentReferralHistoryUserNoteEntity,
  ) = """
    {
      "crn": "${assessmentReferralHistoryNoteSystem.assessment.application.crn}",
      "noms_number": "${assessmentReferralHistoryNoteSystem.assessment.application.nomsNumber}",
      "application_id": "${assessmentReferralHistoryNoteSystem.assessment.application.id}",
      "assessment_id": "${assessmentReferralHistoryNoteSystem.assessment.id}",
      "message": "${assessmentReferralHistoryNoteSystem.message}",
      "created_at": "$CREATED_AT",
      "created_by_user": "${assessmentReferralHistoryNoteSystem.createdByUser.name}",
      "note_type":  "System",
      "system_note_type": "${assessmentReferralHistoryNoteSystem.type}"
    },
    { 
      "crn": "${assessmentReferralHistoryNoteUser.assessment.application.crn}",
      "noms_number": "${assessmentReferralHistoryNoteUser.assessment.application.nomsNumber}",
      "application_id": "${assessmentReferralHistoryNoteUser.assessment.application.id}",
      "assessment_id": "${assessmentReferralHistoryNoteUser.assessment.id}",
      "message": "${assessmentReferralHistoryNoteUser.message}",
      "created_at": "$CREATED_AT",
      "created_by_user": "${assessmentReferralHistoryNoteUser.createdByUser.name}",
      "note_type":  "User",
      "system_note_type": null
      }
  """.trimIndent()

  private fun assessmentReferralHistoryUserNoteEntity(
    temporaryAccomodationAssessment: TemporaryAccommodationAssessmentEntity,
    user: UserEntity,
  ) = assessmentReferralHistoryUserNoteEntityFactory.produceAndPersist {
    withAssessment(temporaryAccomodationAssessment)
    withMessage("some other message")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(user)
  }

  private fun assessmentReferralHistorySystemNoteEntity(
    temporaryAccomodationAssessment: TemporaryAccommodationAssessmentEntity,
    user: UserEntity,
  ) = assessmentReferralHistorySystemNoteEntityFactory.produceAndPersist {
    withAssessment(temporaryAccomodationAssessment)
    withType(ReferralHistorySystemNoteType.READY_TO_PLACE)
    withMessage("Some message")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(user)
  }

  private fun temporaryAccommodationAssessmentJson(
    assessment: TemporaryAccommodationAssessmentEntity,
  ): String =
    """
      {
         "application_id": "${assessment.application.id}",
         "assessment_id": "${assessment.id}",
         "crn": "${assessment.application.crn}",
         "noms_number": "${assessment.application.nomsNumber}",
         "assessor_name": "${assessment.allocatedToUser?.name}",
         "data": $DATA_JSON_SIMPLE,
         "document": $DOCUMENT_JSON_SIMPLE,
         "created_at": "$CREATED_AT",
         "allocated_at": "$ALLOCATED_AT",
         "submitted_at": "$SUBMITTED_AT",
         "reallocated_at": null,
         "due_at": "$DUE_AT",
         "decision": "${AssessmentDecision.REJECTED}",
         "rejection_rationale": "${assessment.rejectionRationale}",
         "is_withdrawn": ${assessment.isWithdrawn},
         "service": "temporary-accommodation",
         "summary_data": $DATA_JSON_SIMPLE,
         "completed_at": "$SUBMITTED_AT",
         "referral_rejection_reason_category": "${assessment.referralRejectionReason?.name}",
         "referral_rejection_reason_detail": "${assessment.referralRejectionReasonDetail}",
         "release_date": "$arrivedAtDateOnly",
         "accommodation_required_from_date": "$arrivedAtDateOnly"      
      }
    """.trimIndent()

  private fun temporaryAccommodationApplicationJson(
    temporaryAccommodationApplication: TemporaryAccommodationApplicationEntity,
  ): String =
    """
    {
        "crn": "${temporaryAccommodationApplication.crn}",
        "noms_number": "${temporaryAccommodationApplication.nomsNumber}",
        "offender_name": "${temporaryAccommodationApplication.name}",
        "data": ${temporaryAccommodationApplication.data},
        "document": ${temporaryAccommodationApplication.document},
        "created_at": "$CREATED_AT",
        "submitted_at": "$SUBMITTED_AT",
        "applications_user_name": "${temporaryAccommodationApplication.createdByUser.name}",
        "conviction_id": ${temporaryAccommodationApplication.convictionId},
        "event_number": "${temporaryAccommodationApplication.eventNumber}",
        "offence_id": "${temporaryAccommodationApplication.offenceId}",
        "probation_region": "${temporaryAccommodationApplication.probationRegion.name}",
        "risk_ratings":${risksJson()},
        "arrival_date": "$ARRIVED_AT",
        "is_duty_to_refer_submitted": ${temporaryAccommodationApplication.isDutyToReferSubmitted},
        "duty_to_refer_submission_date": "$submittedAtDateOnly",
        "duty_to_refer_outcome": null,
        "duty_to_refer_local_authority_area_name": "${temporaryAccommodationApplication.dutyToReferLocalAuthorityAreaName}",
        "is_eligible": ${temporaryAccommodationApplication.isEligible},
        "eligibility_reason": "${temporaryAccommodationApplication.eligibilityReason}",
        "prison_name_on_creation": "${temporaryAccommodationApplication.prisonNameOnCreation}",
        "prison_release_types": "${temporaryAccommodationApplication.prisonReleaseTypes}",
        "person_release_date": "$arrivedAtDateOnly",
        "pdu": "${temporaryAccommodationApplication.pdu}",
        "needs_accessible_property": ${temporaryAccommodationApplication.needsAccessibleProperty},
        "has_history_of_arson": ${temporaryAccommodationApplication.hasHistoryOfArson},
        "is_registered_sex_offender": ${temporaryAccommodationApplication.isRegisteredSexOffender},
        "is_history_of_sexual_offence": ${temporaryAccommodationApplication.isHistoryOfSexualOffence},
        "is_concerning_sexual_behaviour": ${temporaryAccommodationApplication.isConcerningSexualBehaviour},
        "is_concerning_arson_behaviour": ${temporaryAccommodationApplication.isConcerningArsonBehaviour}
    }
    """.trimIndent()

  private fun temporaryAccommodationAssessmentEntity(
    application: TemporaryAccommodationApplicationEntity,
  ): TemporaryAccommodationAssessmentEntity {
    var user = userEntity()
    return temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withAllocatedAt(OffsetDateTime.parse(ALLOCATED_AT))
      withIsWithdrawn(false)
      withAllocatedToUser(userEntity())
      withApplication(application)
      withAssessmentSchema(temporaryAccommodationAssessmentJsonSchemaEntity())
      withDecision(AssessmentDecision.REJECTED)
      withReallocatedAt(null)
      withRejectionRationale("rejected as no good")
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withDueAt(OffsetDateTime.parse(DUE_AT))
      withSummaryData(DATA_JSON_SIMPLE)
      withCompletedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withReferralRejectionReason(
        referralRejectionReasonEntityFactory.produceAndPersist {
          withName(randomStringMultiCaseWithNumbers(6))
          withIsActive(true)
          withServiceScope("TEMPORARY_ACCOMMODATION")
          withSortOrder(1)
        },
      )
      withReferralRejectionReasonDetail("Some Reason Detail")
      withReleaseDate(LocalDate.parse(arrivedAtDateOnly))
      withAccommodationRequiredFromDate(LocalDate.parse(arrivedAtDateOnly))
    }
  }

  private fun temporaryAccommodationAssessmentJsonSchemaEntity(): JsonSchemaEntity =
    temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun temporaryAccommodationApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: UserEntity,
  ): TemporaryAccommodationApplicationEntity {
    val risk1 = personRisks()
    return temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withArrivalDate(OffsetDateTime.parse(ARRIVED_AT))
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withPersonReleaseDate(LocalDate.parse(arrivedAtDateOnly))
      withCreatedByUser(user)
      withApplicationSchema(temporaryAccommodationApplicationJsonSchemaEntity())
      withConvictionId(CONVICTION_ID)
      withName(NAME)
      withCreatedByUser(user)
      withEventNumber(EVENT_NUMBER)
      withOffenceId(OFFENCE_ID)
      withRiskRatings(risk1)
      withDutyToReferLocalAuthorityAreaName(randomStringMultiCaseWithNumbers(10))
      withDutyToReferSubmissionDate(LocalDate.parse(submittedAtDateOnly))
      withDutyToReferOutcome(null)
      withEligiblilityReason("Not eligible")
      withEventNumber("1")
      withHasHistoryOfArson(false)
      withHasRegisteredSexOffender(false)
      withHasHistoryOfSexualOffence(false)
      withIsConcerningArsonBehaviour(false)
      withIsEligible(false)
      withNeedsAccessibleProperty(false)
      withPdu(randomStringMultiCaseWithNumbers(5))
      withProbationRegion(probationRegionEntity())
      withPrisonReleaseTypes("ANY")
      withPrisonNameAtReferral("HMP Birmingham")
    }
  }

  private fun temporaryAccommodationApplicationJsonSchemaEntity() =
    temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
}
