package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

class CAS3SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS3 Information - No Results`() {
    val (offenderDetails, _) = `Given an Offender`()
    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
     {
          "Applications": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS3 Information - Applications`() {
    val (offenderDetails, _) = `Given an Offender`()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}]     
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  private fun temporaryAccommodationApplicationJson(temporaryAccommodationApplication: TemporaryAccommodationApplicationEntity): String =
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
        "duty_to_refer_submission_date": "$SUBMITTED_AT_DATE_ONLY",
        "duty_to_refer_outcome": null,
        "duty_to_refer_local_authority_area_name": "${temporaryAccommodationApplication.dutyToReferLocalAuthorityAreaName}",
        "is_eligible": ${temporaryAccommodationApplication.isEligible},
        "eligibility_reason": "${temporaryAccommodationApplication.eligibilityReason}",
        "prison_name_on_creation": "${temporaryAccommodationApplication.prisonNameOnCreation}",
        "prison_release_types": "${temporaryAccommodationApplication.prisonReleaseTypes}",
        "person_release_date": "$ARRIVED_AT_DATE_ONLY",
        "pdu": "${temporaryAccommodationApplication.pdu}",
        "needs_accessible_property": ${temporaryAccommodationApplication.needsAccessibleProperty},
        "has_history_of_arson": ${temporaryAccommodationApplication.hasHistoryOfArson},
        "is_registered_sex_offender": ${temporaryAccommodationApplication.isRegisteredSexOffender},
        "is_history_of_sexual_offence": ${temporaryAccommodationApplication.isHistoryOfSexualOffence},
        "is_concerning_sexual_behaviour": ${temporaryAccommodationApplication.isConcerningSexualBehaviour},
        "is_concerning_arson_behaviour": ${temporaryAccommodationApplication.isConcerningArsonBehaviour}
    }
    """.trimIndent()

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
      withPersonReleaseDate(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
      withCreatedByUser(user)
      withApplicationSchema(approvedPremisesApplicationJsonSchemaEntity())
      withConvictionId(CONVICTION_ID)
      withName(NAME)
      withCreatedByUser(user)
      withEventNumber(EVENT_NUMBER)
      withOffenceId(OFFENCE_ID)
      withRiskRatings(risk1)
      withDutyToReferLocalAuthorityAreaName(randomStringMultiCaseWithNumbers(10))
      withDutyToReferSubmissionDate(LocalDate.parse(SUBMITTED_AT_DATE_ONLY))
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
}
