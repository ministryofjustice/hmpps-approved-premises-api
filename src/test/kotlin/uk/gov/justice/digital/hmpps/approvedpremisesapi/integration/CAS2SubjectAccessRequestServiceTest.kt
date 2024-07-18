package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

class CAS2SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS2 Information - No Results`() {
    val (offenderDetails, _) = `Given an Offender`()
    val result =
      sarService.getCAS2Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
      {
          "Applications": [ ],
          "ApplicationNotes": [ ],
          "Assessments": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS2 Information - Applications`() {
    val (offenderDetails, _) = `Given an Offender`()
    val user = nomisUserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${Cas2ApplicationsJson(application)}],
      "ApplicationNotes": [ ],
      "Assessments": [ ]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Application with assessment`() {
    val (offenderDetails, _) = `Given an Offender`()
    val user = nomisUserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${Cas2ApplicationsJson(application)}],
      "ApplicationNotes": [ ],
      "Assessments": [${Cas2AssessmentsJson(assessment)}]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Application with Note`() {
    val (offenderDetails, _) = `Given an Offender`()
    val user = nomisUserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${Cas2ApplicationsJson(application)}],
      "ApplicationNotes": [${Cas2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${Cas2AssessmentsJson(assessment)}]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  private fun Cas2ApplicationNotesJson(applicationNotes: Cas2ApplicationNoteEntity): String = """
  {
      "id": "${applicationNotes.id}",
      "crn": "${applicationNotes.application.crn}",
      "noms_number": "${applicationNotes.application.nomsNumber}",
      "application_id": "${applicationNotes.application.id}",
      "assessment_id": "${applicationNotes.assessment!!.id}",
      "created_by_user": "${applicationNotes.getUser().name}",
      "created_by_user_type": "nomis",
      "body": "${applicationNotes.body}",
  }
  """.trimIndent()

  private fun Cas2AssessmentsJson(assessment: Cas2AssessmentEntity): String = """
    {
        "id": "${assessment.id}",
        "crn": "${assessment.application.crn}",
        "noms_number": "${assessment.application.nomsNumber}",
        "application_id": "${assessment.application.id}",
        "created_at": "$CREATED_AT",
        "assessor_name": "${assessment.assessorName}",
        "nacro_referral_id": "${assessment.nacroReferralId}"
    }
  """.trimIndent()

  private fun Cas2ApplicationsJson(application: Cas2ApplicationEntity): String = """
    {
      "id": "${application.id}",
      "crn": "${application.crn}",
      "noms_number": "${application.nomsNumber}",
      "data": ${application.data},
      "document": ${application.document},
      "created_by_user": "${application.createdByUser.name}",
      "created_at": "$CREATED_AT",
      "submitted_at": "$SUBMITTED_AT",
      "referring_prison_code": "${application.referringPrisonCode}",
      "preferred_areas": "${application.preferredAreas}",
      "telephone_number": "${application.telephoneNumber}",
      "hdc_eligibility_date": "$ARRIVED_AT_DATE_ONLY",
      "conditional_release_date": "$ARRIVED_AT_DATE_ONLY",
      "abandoned_at": null
    }
  """.trimIndent()

  private fun cas2ApplicationNoteEntity(
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    user: NomisUserEntity,
  ) = cas2NoteEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedByUser(user)
    withBody("some body text")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  private fun cas2AssessmentEntity(application: Cas2ApplicationEntity) =
    cas2AssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessorName(randomStringMultiCaseWithNumbers(10))
      withNacroReferralId(randomNumberChars(10))
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }

  private fun cas2ApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: NomisUserEntity,
  ) = cas2ApplicationEntityFactory.produceAndPersist {
    withCrn(offenderDetails.otherIds.crn)
    withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
    withCreatedByUser(user)
    withData(DATA_JSON_SIMPLE)
    withDocument(DOCUMENT_JSON_SIMPLE)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withApplicationSchema(
      cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      },
    )
    withReferringPrisonCode(randomStringMultiCaseWithNumbers(3))
    withTelephoneNumber(randomStringMultiCaseWithNumbers(7))

    withConditionalReleaseDate(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
    withHdcEligibilityDate(LocalDate.parse(ARRIVED_AT_DATE_ONLY))
    withPreferredAreas("some areas")
  }

  private fun nomisUserEntity() = nomisUserEntityFactory.produceAndPersist {
    withName(randomStringMultiCaseWithNumbers(12))
    withEmail(randomEmailAddress())
    withNomisUsername(randomStringMultiCaseWithNumbers(7))
    withActiveCaseloadId(randomStringMultiCaseWithNumbers(3))
    withNomisStaffCode(9L)
    withNomisStaffIdentifier(90L)
  }
}
