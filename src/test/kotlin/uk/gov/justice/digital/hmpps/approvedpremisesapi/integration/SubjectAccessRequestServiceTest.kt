package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
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
    const val DEPARTED_AT = "2021-09-23T16:00:00+00:00"
    const val RESPONSE_RECEIVED_AT = "2021-10-23"

    var CREATED_AT_DATE_ONLY = this.CREATED_AT.substring(0..9)
    var ARRIVED_AT_DATE_ONLY = this.ARRIVED_AT.substring(0..9)
    var DEPARTED_AT_DATE_ONLY = this.DEPARTED_AT.substring(0..9)

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
          "approvedPremisesAssessments": [ ],
          "approvedPremisesAssessmentClarificationNotes": [ ],
          "approvedPremisesBookings": [ ]
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
        "approvedPremisesAssessments": [ ],
        "approvedPremisesAssessmentClarificationNotes": [ ],
        "approvedPremisesBookings": [ ]
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
        "approvedPremisesAssessments": [ ],
        "approvedPremisesAssessmentClarificationNotes": [ ],
        "approvedPremisesBookings": [ ]
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
        "approvedPremisesAssessments": ${approvedPremisesAssessmentJson(application,offenderDetails,assessment)},
        "approvedPremisesAssessmentClarificationNotes": [ ],
        "approvedPremisesBookings": [ ]
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
          "approvedPremisesApplications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "approvedPremisesApplicationTimeline" :[ ],
          "approvedPremisesAssessments": ${approvedPremisesAssessmentJson(application,offenderDetails,assessment)},
          "approvedPremisesAssessmentClarificationNotes": ${approvedPremisesAssessmentClarificationNoteJson(assessment,offenderDetails,clarificationNote)},
          "approvedPremisesBookings": [ ]
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
          "approvedPremisesApplications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "approvedPremisesApplicationTimeline" :[ ],
          "approvedPremisesAssessments": [ ],
          "approvedPremisesAssessmentClarificationNotes": [],
          "approvedPremisesBookings": ${bookingsJson(booking)}
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  private fun bookingsJson(booking: BookingEntity): String =
    """[
        {
          "crn": "${booking.crn}",
          "noms_number":"${booking.nomsNumber}",
          "arrival_date":"${booking.arrivalDate}",
          "departure_date":"${booking.departureDate}",
          "original_arrival_date":"${booking.originalArrivalDate}",
          "original_departure_date":"${booking.originalDepartureDate}",
          "created_at":"$CREATED_AT",
          "status":"${booking.status}",
          "premises_name":"${booking.premises.name}",
          "adhoc":${booking.adhoc},
          "key_worker_staff_code":"${booking.keyWorkerStaffCode}",
          "service":"${booking.service}",
          "application_id":"${booking.application?.id}",
          "offline_application_id":${booking.offlineApplication?.let {"${it.id}"} ?: "null" },
          "version":${booking.version}
         }
        ]
    """.trimMargin()

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

  private fun bookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApprovedPremisesApplicationEntity,
  ): BookingEntity {
    var bed = bedEntity()

    var booking = bookingEntityFactory.produceAndPersist {
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
    }
    return booking
  }

  private fun bedEntity() = bedEntityFactory.produceAndPersist {
    withName("a bed")
    withCode("a code")
    withRoom(
      roomEntityFactory.produceAndPersist {
        withCode("room code")
        withName("room name")

        withPremises(
          approvedPremisesEntityFactory.produceAndPersist {
            withName("a premises")
            withApCode("AP Code")
            withLocalAuthorityArea(
              localAuthorityEntityFactory.produceAndPersist {
                withName("An LAA")
                withIdentifier("LAA ID")
              },
            )
            withProbationRegion(
              probationRegionEntityFactory.produceAndPersist {
                withName("Probation Region")
                withApArea(
                  apAreaEntityFactory.produceAndPersist {
                    withName("Probation Area")
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
