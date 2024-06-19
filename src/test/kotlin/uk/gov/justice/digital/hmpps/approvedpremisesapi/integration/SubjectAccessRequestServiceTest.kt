package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class SubjectAccessRequestServiceTest : IntegrationTestBase() {

  companion object {

    val START_DATE: LocalDateTime = LocalDateTime.of(2018, 9, 30, 0, 0, 0)
    val END_DATE: LocalDateTime = LocalDateTime.of(2024, 9, 30, 0, 0, 0)
    const val CREATED_AT = "2021-09-18T16:00:00+00:00"
    const val SUBMITTED_AT = "2021-10-19T16:00:00+00:00"
    const val ARRIVED_AT = "2021-09-20T16:00:00+00:00"
    const val ALLOCATED_AT = "2021-09-21T16:00:00+00:00"
    const val CREATED_AT_NO_TZ = "2021-09-18T16:00:00"
    const val DUE_AT = "2021-09-22T16:00:00+00:00"

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
      """ {
          "approvedPremisesApplications": [ ],
          "approvedPremisesApplicationTimeline": [ ],
          "approvedPremisesAssessments": [ ]
 
        }
      """,
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
        "approvedPremisesApplications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "approvedPremisesApplicationTimeline" :[ ],
        "approvedPremisesAssessments": [ ]

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
    val timelineNotes = applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(application.id)
      withBody("Some random note about this application")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withCreatedBy(application.createdByUser)
    }

    val result = sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """{
        "approvedPremisesApplications": ${approvedPremisesApplicationsJson(application, offender)},
        "approvedPremisesApplicationTimeline": ${
    approvedPremisesApplicationTimelineNotesJson(
      application,
      timelineNotes,
      offender,
    )
    },
    "approvedPremisesAssessments": [ ]
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
        "approvedPremisesApplications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "approvedPremisesApplicationTimeline" :[ ],
        "approvedPremisesAssessments": ${approvedPremisesAssessmentJson(application,offenderDetails,assessment)}
      }
       """

    assertJsonEquals(expectedJson, result)
  }

  private fun approvedPremisesAssessment(
    application: ApprovedPremisesApplicationEntity,
  ): ApprovedPremisesAssessmentEntity {
    return approvedPremisesAssessmentEntityFactory.produceAndPersist {
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

  private fun approvedPremisesApplicationsJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
  ) = """[
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
  ): String =
    """
      [ 
        {
        "application_id":"${application.id}",
        "service":"approved-premises",
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

  private fun risksJson(): String = """
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

  private fun approvedPremisesApplicationJsonSchemaEntity(): ApprovedPremisesApplicationJsonSchemaEntity =
    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun approvedPremisesAssessmentJsonSchemaEntity(): ApprovedPremisesAssessmentJsonSchemaEntity =
    approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun cas1ApplicationUserDetails(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname_applicant_user@noname.net")
    }

  private fun cas1CaseManagerUserDetails(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname@noname.net")
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
    val applicantUserDetails = cas1ApplicationUserDetails()
    val caseManagerUserDetails = cas1CaseManagerUserDetails()
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
}
