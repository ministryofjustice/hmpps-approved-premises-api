package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class CAS2SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS2 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS2Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNull(result)
  }

  @Test
  fun `Get CAS2 Information - null date Check`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS2Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)

    assertNull(result)
  }

  @Test
  fun `Get CAS2 Information - Applications`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2UserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    assertNotNull(result)

    val expectedJson = """
   {
      "Applications": [${cas2ApplicationsJson(application)}],
      "ApplicationNotes": [],
      "Assessments": [],
      "StatusUpdates": [],
      "StatusUpdateDetails": [],
      "DomainEvents":  [],
      "DomainEventsMetadata": []
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Application with assessment`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2UserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    assertNotNull(result)

    val expectedJson = """
   {
      "Applications": [${cas2ApplicationsJson(application)}],
      "ApplicationNotes": [],
      "Assessments": [${cas2AssessmentsJson(assessment)}],
      "StatusUpdates": [],
      "StatusUpdateDetails": [],
      "DomainEvents":  [],
      "DomainEventsMetadata": []
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Application with Note`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2UserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    assertNotNull(result)

    val expectedJson = """
   {
      "Applications": [${cas2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2AssessmentsJson(assessment)}],
      "StatusUpdates": [],
      "StatusUpdateDetails": [],
      "DomainEvents":  [],
      "DomainEventsMetadata": []

   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Application with Note and Status update`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2UserEntity()
    val externalAssessor = externalUserEntity()
    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2StatusUpdateEntity(application, assessment, externalAssessor)
    val statusUpdateDetail = cas2StatusUpdateDetailEntity(statusUpdate)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    assertNotNull(result)

    val expectedJson = """
   {
      "Applications": [${cas2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2AssessmentsJson(assessment)}],
      "StatusUpdates": [${cas2StatusUpdatesJson(statusUpdate)}],
      "StatusUpdateDetails": [${cas2StatusUpdateDetails(statusUpdateDetail)}],
      "DomainEvents":  [],
      "DomainEventsMetadata": []
      
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 Information - Domain Events`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2UserEntity()
    val externalAssessor = externalUserEntity()
    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2StatusUpdateEntity(application, assessment, externalAssessor)
    val statusUpdateDetail = cas2StatusUpdateDetailEntity(statusUpdate)
    val domainEvent = domainEventEntity(offenderDetails, application.id, assessment.id, null, ServiceName.cas2)

    val result = sarService.getCAS2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    assertNotNull(result)

    val expectedJson = """
   {
      "Applications": [${cas2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2AssessmentsJson(assessment)}],
      "StatusUpdates": [${cas2StatusUpdatesJson(statusUpdate)}],
      "StatusUpdateDetails": [${cas2StatusUpdateDetails(statusUpdateDetail)}],
      "DomainEvents": [${domainEventJson(domainEvent, null)}],
      "DomainEventsMetadata": [${domainEventsMetadataJson(domainEvent)}]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  private fun cas2StatusUpdateDetails(statusUpdateDetail: Cas2StatusUpdateDetailEntity): String = """
    {
        "crn": "${statusUpdateDetail.statusUpdate.application.crn}",
        "noms_number": "${statusUpdateDetail.statusUpdate.application.nomsNumber}", 
        "status_update_id": "${statusUpdateDetail.statusUpdate.id}",
        "application_id": "${statusUpdateDetail.statusUpdate.application.id}",
        "assessment_id": "${statusUpdateDetail.statusUpdate.assessment!!.id}",
        "status_label": "${statusUpdateDetail.statusUpdate.label}",
        "detail_label": "${statusUpdateDetail.label}",
        "created_at": "${statusUpdateDetail.createdAt.toStandardisedFormat()}"
    }
  """.trimIndent()

  private fun cas2StatusUpdatesJson(statusUpdate: Cas2StatusUpdateEntity): String = """
    {
      	"id": "${statusUpdate.id}",
      	"crn": "${statusUpdate.application.crn}",
      	"noms_number": "${statusUpdate.application.nomsNumber}", 
      	"application_id": "${statusUpdate.application.id}",
      	"assessment_id": "${statusUpdate.assessment!!.id}",
      	"assessor_name": "${statusUpdate.assessor.name}",
        "assessor_origin": "${statusUpdate.assessor.origin}",
      	"created_at": "${statusUpdate.createdAt.toStandardisedFormat()}",
        "description": "${statusUpdate.description}",
        "label": "${statusUpdate.label}"
    }    
  """.trimIndent()

  private fun cas2ApplicationNotesJson(applicationNotes: Cas2ApplicationNoteEntity): String = """
  {
      "id": "${applicationNotes.id}",
      "crn": "${applicationNotes.application.crn}",
      "noms_number": "${applicationNotes.application.nomsNumber}",
      "application_id": "${applicationNotes.application.id}",
      "assessment_id": "${applicationNotes.assessment!!.id}",
      "created_by_user": "${applicationNotes.getUser().name}",
      "created_by_user_type": "NOMIS",
      "body": "${applicationNotes.body}"
  }
  """.trimIndent()

  private fun cas2AssessmentsJson(assessment: Cas2AssessmentEntity): String = """
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

  private fun cas2ApplicationsJson(application: Cas2ApplicationEntity): String = """
    {
      "id": "${application.id}",
      "crn": "${application.crn}",
      "noms_number": "${application.nomsNumber}",
      "data": ${application.data},
      "document": ${application.document},
      "created_by_user": "${application.createdByUser!!.name}",
      "created_at": "$CREATED_AT",
      "submitted_at": "$SUBMITTED_AT",
      "referring_prison_code": "${application.referringPrisonCode}",
      "preferred_areas": "${application.preferredAreas}",
      "telephone_number": "${application.telephoneNumber}",
      "hdc_eligibility_date": "$arrivedAtDateOnly",
      "conditional_release_date": "$arrivedAtDateOnly",
      "abandoned_at": null,
      "application_origin": "${application.applicationOrigin}",
      "bail_hearing_date": null,
    }
  """.trimIndent()

  private fun cas2ApplicationNoteEntity(
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

  private fun cas2StatusUpdateEntity(
    application: Cas2ApplicationEntity,
    assessment: Cas2AssessmentEntity,
    externalAssessor: ExternalUserEntity,
  ): Cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withStatusId(UUID.randomUUID())
    withAssessor(externalAssessor)
    withLabel("Some Label")
    withDescription("Some Description")
  }

  private fun cas2StatusUpdateDetailEntity(statusUpdate: Cas2StatusUpdateEntity): Cas2StatusUpdateDetailEntity = cas2StatusUpdateDetailEntityFactory.produceAndPersist {
    withStatusUpdate(statusUpdate)
    withLabel("Some detailed label")
    withStatusDetailId(UUID.randomUUID())
  }

  private fun externalUserEntity() = externalUserEntityFactory.produceAndPersist {
    withName(randomStringMultiCaseWithNumbers(12))
    withEmail(randomEmailAddress())
    withOrigin("NACRO")
    withUsername(randomStringMultiCaseWithNumbers(10))
  }

  private fun cas2AssessmentEntity(application: Cas2ApplicationEntity) = cas2AssessmentEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessorName(randomStringMultiCaseWithNumbers(10))
    withNacroReferralId(randomNumberChars(10))
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  private fun cas2ApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: Cas2UserEntity,
  ) = cas2ApplicationEntityFactory.produceAndPersist {
    withCrn(offenderDetails.otherIds.crn)
    withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
    withCreatedByUser(user)
    withData(DATA_JSON_SIMPLE)
    withDocument(DOCUMENT_JSON_SIMPLE)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withReferringPrisonCode(randomStringMultiCaseWithNumbers(3))
    withTelephoneNumber(randomStringMultiCaseWithNumbers(7))

    withConditionalReleaseDate(LocalDate.parse(arrivedAtDateOnly))
    withHdcEligibilityDate(LocalDate.parse(arrivedAtDateOnly))
    withPreferredAreas("some areas")
  }

  private fun cas2UserEntity() = cas2UserEntityFactory.produceAndPersist {
    withName(randomStringMultiCaseWithNumbers(12))
    withEmail(randomEmailAddress())
    withUsername(randomStringMultiCaseWithNumbers(7))
    withActiveNomisCaseloadId(randomStringMultiCaseWithNumbers(3))
    withNomisStaffCode(9L)
    withNomisStaffIdentifier(90L)
  }
}
