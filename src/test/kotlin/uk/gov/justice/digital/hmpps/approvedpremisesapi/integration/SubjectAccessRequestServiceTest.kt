package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequest.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class SubjectAccessRequestServiceTest : IntegrationTestBase() {

  companion object {
    const val CRN = "CRN123"
    const val NOMS_NUMBER = "A123456"
    val START_DATE: LocalDateTime = java.time.LocalDateTime.of(2018, 9, 30, 0, 0, 0)
    val END_DATE: LocalDateTime = java.time.LocalDateTime.of(2024, 9, 30, 0, 0, 0)
    const val CREATED_AT = "2021-09-25T16:00:00+00:00"
    const val SUBMITTED_AT = "2021-10-25T16:00:00+00:00"
    const val ARRIVED_AT = "2021-09-26T16:00:00+00:00"

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
    val result = sarService.getSarResult(CRN, NOMS_NUMBER, START_DATE, END_DATE)
    assertJsonEquals(
      """ {
          "approvedPremisesApplications": [ ]
        }
      """,
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val risk1 = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      )
      .produce()

    val risksJson = """
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
    },
    """.trimIndent()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(
            apAreaEntityFactory.produceAndPersist {
            },
          )
        },
      )
    }

    val caseManagerUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname@noname.net")
    }

    val applicantUserDetails = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname_applicant_user@noname.net")
    }

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(CRN)
      withNomsNumber(NOMS_NUMBER)
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

    val result = sarService.getSarResult(CRN, NOMS_NUMBER, START_DATE, END_DATE)

    val expectedJson = """
      {
        "approvedPremisesApplications": 
        [
          {
            "id": "${application.id}",
            "name": "$NAME",
            "crn": "$CRN",
            "noms_number": "$NOMS_NUMBER",
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
            "risk_ratings": $risksJson 
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
      }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  private fun assertJsonEquals(
    expected: String,
    actual: String,
  ) {
    JSONAssert.assertEquals(
      expected.trimMargin(),
      actual,
      JSONCompareMode.NON_EXTENSIBLE,
    )
  }
}
