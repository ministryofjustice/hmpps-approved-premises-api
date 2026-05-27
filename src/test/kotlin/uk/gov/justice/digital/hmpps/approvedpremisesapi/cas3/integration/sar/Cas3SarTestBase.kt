package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

open class Cas3SarTestBase : SubjectAccessRequestServiceTestBase() {

  companion object {
    const val CAS3_DATA_PATH = "db/seed/dev+test/cas3_application_data"

    val CAS3_APPLICATION_DATA by lazy { readResource("$CAS3_DATA_PATH/application_data.json") }
    val CAS3_APPLICATION_DOCUMENT by lazy { readResource("$CAS3_DATA_PATH/application_document.json") }
  }

  protected fun assessmentReferralHistoryNotesJson(
    assessmentReferralHistoryNoteSystem: AssessmentReferralHistorySystemNoteEntity,
    assessmentReferralHistoryNoteUser: AssessmentReferralHistoryUserNoteEntity,
  ) = """
    {
      "crn": "${assessmentReferralHistoryNoteSystem.assessment.application.crn}",
      "noms_number": "${assessmentReferralHistoryNoteSystem.assessment.application.nomsNumber}",
      "message": "${assessmentReferralHistoryNoteSystem.message}",
      "created_at": "$CREATED_AT",
      "created_by_user": "${assessmentReferralHistoryNoteSystem.createdByUser.name}",
      "note_type":  "System",
      "system_note_type": "${assessmentReferralHistoryNoteSystem.type}"
    },
    { 
      "crn": "${assessmentReferralHistoryNoteUser.assessment.application.crn}",
      "noms_number": "${assessmentReferralHistoryNoteUser.assessment.application.nomsNumber}",
      "message": "${assessmentReferralHistoryNoteUser.message}",
      "created_at": "$CREATED_AT",
      "created_by_user": "${assessmentReferralHistoryNoteUser.createdByUser.name}",
      "note_type":  "User",
      "system_note_type": null
      }
  """.trimIndent()

  protected fun assessmentReferralHistoryUserNoteEntity(
    temporaryAccomodationAssessment: TemporaryAccommodationAssessmentEntity,
    user: UserEntity,
  ) = assessmentReferralHistoryUserNoteEntityFactory.produceAndPersist {
    withAssessment(temporaryAccomodationAssessment)
    withMessage("some other message")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(user)
  }

  protected fun assessmentReferralHistorySystemNoteEntity(
    temporaryAccomodationAssessment: TemporaryAccommodationAssessmentEntity,
    user: UserEntity,
  ) = assessmentReferralHistorySystemNoteEntityFactory.produceAndPersist {
    withAssessment(temporaryAccomodationAssessment)
    withType(ReferralHistorySystemNoteType.READY_TO_PLACE)
    withMessage("Some message")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(user)
  }

  protected fun temporaryAccommodationAssessmentJson(
    assessment: TemporaryAccommodationAssessmentEntity,
  ): String =
    """
      {
         "crn": "${assessment.application.crn}",
         "noms_number": "${assessment.application.nomsNumber}",
         "assessor_name": "${assessment.allocatedToUser?.name}",
         "document": $DOCUMENT_JSON_SIMPLE,
         "created_at": "$CREATED_AT",
         "allocated_at": "$ALLOCATED_AT",
         "submitted_at": "$SUBMITTED_AT",
         "reallocated_at": null,
         "due_at": "$DUE_AT",
         "decision": "${AssessmentDecision.REJECTED}",
         "rejection_rationale": "${assessment.rejectionRationale}",
         "is_withdrawn": ${assessment.isWithdrawn},
         "summary_data": $DATA_JSON_SIMPLE,
         "completed_at": "$SUBMITTED_AT",
         "referral_rejection_reason_category": "${assessment.referralRejectionReason?.name}",
         "referral_rejection_reason_detail": "${assessment.referralRejectionReasonDetail}",
         "release_date": "$arrivedAtDateOnly",
         "accommodation_required_from_date": "$arrivedAtDateOnly"      
      }
    """.trimIndent()

  protected fun temporaryAccommodationApplicationJson(
    temporaryAccommodationApplication: TemporaryAccommodationApplicationEntity,
  ): String =
    """
    {
        "crn": "${temporaryAccommodationApplication.crn}",
        "noms_number": "${temporaryAccommodationApplication.nomsNumber}",
        "offender_name": "${temporaryAccommodationApplication.name}",
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
        "pdu": "${temporaryAccommodationApplication.probationDeliveryUnit?.name}",
        "needs_accessible_property": ${temporaryAccommodationApplication.needsAccessibleProperty},
        "has_history_of_arson": ${temporaryAccommodationApplication.hasHistoryOfArson},
        "is_registered_sex_offender": ${temporaryAccommodationApplication.isRegisteredSexOffender},
        "is_history_of_sexual_offence": ${temporaryAccommodationApplication.isHistoryOfSexualOffence},
        "is_concerning_sexual_behaviour": ${temporaryAccommodationApplication.isConcerningSexualBehaviour},
        "is_concerning_arson_behaviour": ${temporaryAccommodationApplication.isConcerningArsonBehaviour}
    }
    """.trimIndent()

  protected fun temporaryAccommodationAssessmentEntity(
    application: TemporaryAccommodationApplicationEntity,
    allocatedToUser: UserEntity = userEntity(),
    referralRejectionReasonName: String = randomStringMultiCaseWithNumbers(6),
  ): TemporaryAccommodationAssessmentEntity = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    withData(DATA_JSON_SIMPLE)
    withDocument(DOCUMENT_JSON_SIMPLE)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedAt(OffsetDateTime.parse(ALLOCATED_AT))
    withIsWithdrawn(false)
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withDecision(AssessmentDecision.REJECTED)
    withReallocatedAt(null)
    withRejectionRationale("rejected as no good")
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withDueAt(OffsetDateTime.parse(DUE_AT))
    withSummaryData(DATA_JSON_SIMPLE)
    withCompletedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withReferralRejectionReason(
      referralRejectionReasonEntityFactory.produceAndPersist {
        withName(referralRejectionReasonName)
        withIsActive(true)
      },
    )
    withReferralRejectionReasonDetail("Some Reason Detail")
    withReleaseDate(LocalDate.parse(arrivedAtDateOnly))
    withAccommodationRequiredFromDate(LocalDate.parse(arrivedAtDateOnly))
  }

  internal fun temporaryAccommodationApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: UserEntity,
    dutyToReferLocalAuthorityAreaName: String = randomStringMultiCaseWithNumbers(10),
    probationRegionName: String = "Probation Region ${randomStringMultiCaseWithNumbers(5)}",
    probationDeliveryUnitName: String = randomStringMultiCaseWithNumbers(8),
    data: String = DATA_JSON_SIMPLE,
    document: String = DOCUMENT_JSON_SIMPLE,
  ): TemporaryAccommodationApplicationEntity {
    val risk1 = personRisks()
    return temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withArrivalDate(OffsetDateTime.parse(ARRIVED_AT))
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withData(data)
      withDocument(document)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withPersonReleaseDate(LocalDate.parse(arrivedAtDateOnly))
      withCreatedByUser(user)
      withConvictionId(CONVICTION_ID)
      withName(NAME)
      withCreatedByUser(user)
      withEventNumber(EVENT_NUMBER)
      withOffenceId(OFFENCE_ID)
      withRiskRatings(risk1)
      withDutyToReferLocalAuthorityAreaName(dutyToReferLocalAuthorityAreaName)
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
      withProbationRegion(probationRegionEntity(name = probationRegionName))
      withPrisonReleaseTypes("ANY")
      withPrisonNameAtReferral("HMP Birmingham")
      withProbationDeliveryUnit(probationDeliveryUnitEntity(user, name = probationDeliveryUnitName))
    }
  }
}
