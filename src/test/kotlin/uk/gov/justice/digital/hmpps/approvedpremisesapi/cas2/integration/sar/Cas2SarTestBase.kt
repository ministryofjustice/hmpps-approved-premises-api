package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar.Cas3SarComplianceTest.Companion.TEST_CREATED_BY_USER_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

open class Cas2SarTestBase : SubjectAccessRequestServiceTestBase() {

  companion object {
    const val CAS2_DATA_PATH = "db/seed/dev+test/cas2_application_data"
    const val CAS2V2_DATA_PATH = "db/seed/dev+test/cas2v2_application_data"

    val CAS2_APPLICATION_DATA by lazy { readResource("$CAS2_DATA_PATH/data_A1234AI.json") }
    val CAS2_APPLICATION_DOCUMENT by lazy { readResource("$CAS2_DATA_PATH/document_A1234AI.json") }

    val CAS2V2_APPLICATION_DATA by lazy { readResource("$CAS2V2_DATA_PATH/data_A1234AX.json") }
    val CAS2V2_APPLICATION_DOCUMENT by lazy { readResource("$CAS2V2_DATA_PATH/document_A1234AX.json") }
  }

  protected fun cas2ApplicationNotesJson(applicationNotes: Cas2ApplicationNoteEntity): String = """
  {
      "created_by_user": "${applicationNotes.createdByUser.name}",
      "body": "${applicationNotes.body}"
  }
  """.trimIndent()

  protected fun cas2AssessmentsJson(assessment: Cas2AssessmentEntity): String = """
    {
        "created_at": "$CREATED_AT",
        "assessor_name": "${assessment.assessorName}",
        "nacro_referral_id": "${assessment.nacroReferralId}"
    }
  """.trimIndent()

  protected fun cas2ApplicationsJson(application: Cas2ApplicationEntity): String = """
{
  "document": ${application.document},
  "created_by_user": "${application.createdByUser.name}",
  "created_at": "$CREATED_AT",
  "submitted_at": "$SUBMITTED_AT",
  "referring_prison_code": "${application.referringPrisonCode}",
  "preferred_areas": "${application.preferredAreas}",
  "telephone_number": "${application.telephoneNumber}",
  "hdc_eligibility_date": "$arrivedAtDateOnly",
  "conditional_release_date": "$arrivedAtDateOnly",
  "abandoned_at": null,
  "application_origin": "${application.applicationOrigin}",
  "bail_hearing_date": ${if (application.serviceOrigin == Cas2ServiceOrigin.BAIL) "\"${application.bailHearingDate}\"" else "null"}
  ${
    if (application.serviceOrigin == Cas2ServiceOrigin.BAIL) {
      ",\n  \"cohort_long_display_name\": ${application.cohort?.longDisplayName?.let { "\"$it\"" } ?: "null"}"
    } else {
      ""
    }
  }
}
  """.trimIndent()

  protected fun cas2ApplicationNoteEntity(
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    user: Cas2UserEntity,
  ) = cas2NoteEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedByUser(user)
    withBody("some body text")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  protected fun cas2AssessmentEntity(
    application: Cas2ApplicationEntity,
    serviceOrigin: Cas2ServiceOrigin = Cas2ServiceOrigin.HDC,
    assessorName: String = randomStringMultiCaseWithNumbers(10),
    nacroReferralId: String = randomNumberChars(10),
  ) = cas2AssessmentEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessorName(assessorName)
    withNacroReferralId(nacroReferralId)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT).withOffsetSameInstant(ZoneOffset.UTC))
    withServiceOrigin(serviceOrigin)
  }

  protected fun cas2ApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: Cas2UserEntity,
    serviceOrigin: Cas2ServiceOrigin = Cas2ServiceOrigin.HDC,
    referringPrisonCode: String = randomStringMultiCaseWithNumbers(3),
    telephoneNumber: String = randomStringMultiCaseWithNumbers(7),
    data: String = DATA_JSON_SIMPLE,
    document: String = DOCUMENT_JSON_SIMPLE,
    cohort: Cas2Cohort? = null,
  ) = cas2ApplicationEntityFactory.produceAndPersist {
    withCrn(offenderDetails.otherIds.crn)
    withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
    withCreatedByUser(user)
    withData(data)
    withDocument(document)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withReferringPrisonCode(referringPrisonCode)
    withTelephoneNumber(telephoneNumber)
    withConditionalReleaseDate(LocalDate.parse(arrivedAtDateOnly))
    withHdcEligibilityDate(LocalDate.parse(arrivedAtDateOnly))
    if (serviceOrigin == Cas2ServiceOrigin.BAIL) {
      withBailHearingDate(LocalDate.parse(arrivedAtDateOnly))
      withCohort(cohort)
    }
    withPreferredAreas("some areas")
    withServiceOrigin(serviceOrigin)
  }

  protected fun cas2NomisUserEntity(
    serviceOrigin: Cas2ServiceOrigin = Cas2ServiceOrigin.HDC,
    name: String = randomStringMultiCaseWithNumbers(12),
  ) = cas2UserEntityFactory.produceAndPersist {
    withName(name)
    withEmail(randomEmailAddress())
    withUsername(TEST_CREATED_BY_USER_NAME)
    withActiveNomisCaseloadId(randomStringMultiCaseWithNumbers(3))
    withNomisStaffCode(9L)
    withNomisStaffIdentifier(90L)
    withServiceOrigin(serviceOrigin)
  }

  protected fun cas2ExternalUserEntity(
    name: String = randomStringMultiCaseWithNumbers(12),
  ) = cas2UserEntityFactory.produceAndPersist {
    withName(name)
    withEmail(randomEmailAddress())
    withUsername(randomStringMultiCaseWithNumbers(7))
    withUserType(Cas2UserType.EXTERNAL)
    withExternalType("NACRO")
  }
}
