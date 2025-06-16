package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2v2SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS2 v2 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS2v2Result(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )
    assertJsonEquals(
      """ 
      {
          "Applications": [ ],
          "ApplicationNotes": [ ],
          "Assessments": [ ],
          "StatusUpdates": [ ],
          "StatusUpdateDetails": [ ],
          "DomainEvents":  [ ],
          "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS2 v2 Information - null date Check`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS2Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)
    assertJsonEquals(
      """ 
      {
          "Applications": [ ],
          "ApplicationNotes": [ ],
          "Assessments": [ ],
          "StatusUpdates": [ ],
          "StatusUpdateDetails": [ ],
          "DomainEvents":  [ ],
          "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS2 v2 Information - Applications`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = nomisUserEntity()

    val application = cas2v2ApplicationEntity(offenderDetails, user)

    val result = sarService.getCAS2v2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${cas2v2ApplicationsJson(application)}],
      "ApplicationNotes": [ ],
      "Assessments": [ ],
      "StatusUpdates": [ ],
      "StatusUpdateDetails": [ ],
      "DomainEvents":  [ ],
      "DomainEventsMetadata": [ ]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 v2 Information - Application with assessment`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = nomisUserEntity()

    val application = cas2v2ApplicationEntity(offenderDetails, user)
    val assessment = cas2v2AssessmentEntity(application)

    val result = sarService.getCAS2v2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${cas2v2ApplicationsJson(application)}],
      "ApplicationNotes": [ ],
      "Assessments": [${cas2v2AssessmentsJson(assessment)}],
      "StatusUpdates": [ ],
      "StatusUpdateDetails": [ ],
      "DomainEvents":  [ ],
      "DomainEventsMetadata": [ ]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 v2 Information - Application with Note`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = nomisUserEntity()

    val application = cas2v2ApplicationEntity(offenderDetails, user)
    val assessment = cas2v2AssessmentEntity(application)

    val applicationNotes = cas2v2ApplicationNoteEntity(application, assessment, user)

    val result = sarService.getCAS2v2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${cas2v2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2v2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2v2AssessmentsJson(assessment)}],
      "StatusUpdates": [ ],
      "StatusUpdateDetails": [ ],
      "DomainEvents":  [ ],
      "DomainEventsMetadata": [ ]

   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 v2 Information - Application with Note and Status update`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = nomisUserEntity()
    val application = cas2v2ApplicationEntity(offenderDetails, user)
    val assessment = cas2v2AssessmentEntity(application)

    val applicationNotes = cas2v2ApplicationNoteEntity(application, assessment, user)

    val statusUpdate = cas2v2StatusUpdateEntity(application, assessment, user)
    val statusUpdateDetail = cas2StatusUpdateDetailEntity(statusUpdate)

    val result = sarService.getCAS2v2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${cas2v2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2v2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2v2AssessmentsJson(assessment)}],
      "StatusUpdates": [${cas2v2StatusUpdatesJson(statusUpdate)}],
      "StatusUpdateDetails": [${cas2v2StatusUpdateDetails(statusUpdateDetail)}],
      "DomainEvents":  [ ],
      "DomainEventsMetadata": [ ]
      
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS2 v2 Information - Domain Events`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = nomisUserEntity()
    val application = cas2v2ApplicationEntity(offenderDetails, user)
    val assessment = cas2v2AssessmentEntity(application)

    val applicationNotes = cas2v2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2v2StatusUpdateEntity(application, assessment, user)
    val statusUpdateDetail = cas2StatusUpdateDetailEntity(statusUpdate)
    val domainEvent = domainEventEntity(offenderDetails, application.id, assessment.id, null, ServiceName.cas2v2)

    val result = sarService.getCAS2v2Result(
      offenderDetails.otherIds.crn,
      offenderDetails.otherIds.nomsNumber,
      START_DATE,
      END_DATE,
    )

    val expectedJson = """
   {
      "Applications": [${cas2v2ApplicationsJson(application)}],
      "ApplicationNotes": [${cas2v2ApplicationNotesJson(applicationNotes)}],
      "Assessments": [${cas2v2AssessmentsJson(assessment)}],
      "StatusUpdates": [${cas2v2StatusUpdatesJson(statusUpdate)}],
      "StatusUpdateDetails": [${cas2v2StatusUpdateDetails(statusUpdateDetail)}],
      "DomainEvents": [${domainEventJson(domainEvent,null)}],
      "DomainEventsMetadata": [${domainEventsMetadataJson(domainEvent)}]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  private fun cas2v2StatusUpdateDetails(statusUpdateDetail: Cas2v2StatusUpdateDetailEntity): String = """
    {
        "crn": "${statusUpdateDetail.statusUpdate.application.crn}",
        "noms_number": "${statusUpdateDetail.statusUpdate.application.nomsNumber}", 
        "status_update_id": "${statusUpdateDetail.statusUpdate.id}",
        "application_id": "${statusUpdateDetail.statusUpdate.application.id}",
        "assessment_id": "${statusUpdateDetail.statusUpdate.assessment!!.id}",
        "status_label": "${statusUpdateDetail.statusUpdate.label}",
        "detail_label": "${statusUpdateDetail.label}",
        "created_at": "${statusUpdateDetail.createdAt!!.withOffsetSameInstant(ZoneOffset.UTC).toStandardisedFormat()}"
    }
  """.trimIndent()

  private fun cas2v2StatusUpdatesJson(statusUpdate: Cas2v2StatusUpdateEntity): String = """
    {
      	"id": "${statusUpdate.id}",
      	"crn": "${statusUpdate.application.crn}",
      	"noms_number": "${statusUpdate.application.nomsNumber}", 
      	"application_id": "${statusUpdate.application.id}",
      	"assessment_id": "${statusUpdate.assessment!!.id}",
      	"assessor_name": "${statusUpdate.assessor.name}",
        "created_at": "${statusUpdate.createdAt.withOffsetSameInstant(ZoneOffset.UTC).toStandardisedFormat()}",
        "description": "${statusUpdate.description}",
        "label": "${statusUpdate.label}"
    }    
  """.trimIndent()
//              	"created_at": "${statusUpdate.createdAt.toStandardisedFormat()}",

  private fun cas2v2ApplicationNotesJson(applicationNotes: Cas2v2ApplicationNoteEntity): String = """
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

  private fun cas2v2AssessmentsJson(assessment: Cas2v2AssessmentEntity): String = """
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

  private fun cas2v2ApplicationsJson(application: Cas2v2ApplicationEntity): String = """
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
      "hdc_eligibility_date": "$arrivedAtDateOnly",
      "conditional_release_date": "$arrivedAtDateOnly",
      "abandoned_at": null,
      "application_origin": "${application.applicationOrigin}",
      "bail_hearing_date": "${application.bailHearingDate}",
    }
  """.trimIndent()

  private fun cas2v2ApplicationNoteEntity(
    application: Cas2v2ApplicationEntity,
    assessment: Cas2v2AssessmentEntity,
    user: Cas2v2UserEntity,
  ) = cas2v2NoteEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedByUser(user)
    withBody("some body text")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  private fun cas2v2StatusUpdateEntity(
    application: Cas2v2ApplicationEntity,
    assessment: Cas2v2AssessmentEntity,
    externalAssessor: Cas2v2UserEntity,
  ): Cas2v2StatusUpdateEntity = cas2v2StatusUpdateEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withStatusId(UUID.randomUUID())
    withAssessor(externalAssessor)
    withLabel("Some Label")
    withDescription("Some Description")
  }

  private fun cas2StatusUpdateDetailEntity(statusUpdate: Cas2v2StatusUpdateEntity): Cas2v2StatusUpdateDetailEntity = cas2v2StatusUpdateDetailEntityFactory.produceAndPersist {
    withStatusUpdate(statusUpdate)
    withLabel("Some detailed label")
    withStatusDetailId(UUID.randomUUID())
    withCreatedAt(OffsetDateTime.parse(CREATED_AT).withOffsetSameInstant(ZoneOffset.UTC))
  }

  private fun cas2v2AssessmentEntity(application: Cas2v2ApplicationEntity) = cas2v2AssessmentEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessorName(randomStringMultiCaseWithNumbers(10))
    withNacroReferralId(randomNumberChars(10))
    withCreatedAt(OffsetDateTime.parse(CREATED_AT).withOffsetSameInstant(ZoneOffset.UTC))
  }

  private fun cas2v2ApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    user: Cas2v2UserEntity,
  ): Cas2v2ApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
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

    withConditionalReleaseDate(LocalDate.parse(arrivedAtDateOnly))
    withHdcEligibilityDate(LocalDate.parse(arrivedAtDateOnly))
    withBailHearingDate(LocalDate.parse(arrivedAtDateOnly))
    withPreferredAreas("some areas")
  }

  private fun nomisUserEntity() = cas2v2UserEntityFactory.produceAndPersist {
    withName(randomStringMultiCaseWithNumbers(12))
    withEmail(randomEmailAddress())
    withUserType(Cas2v2UserType.NOMIS)
//    withNomisUsername(randomStringMultiCaseWithNumbers(7))
//    withActiveCaseloadId(randomStringMultiCaseWithNumbers(3))
    withNomisStaffCode(9L)
    withNomisStaffIdentifier(90L)
  }
}
