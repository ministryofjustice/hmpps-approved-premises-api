package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
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
    withSummaryData("{\"isAbleToShare\": false, \"releaseType\": \"licence\"}")
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
      withIsConcerningSexualBehaviour(false)
      withDutyToReferOutcome("pending")
    }
  }
}
