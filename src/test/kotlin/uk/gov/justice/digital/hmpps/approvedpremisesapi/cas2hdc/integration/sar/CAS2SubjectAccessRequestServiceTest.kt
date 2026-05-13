package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

class CAS2SubjectAccessRequestServiceTest : Cas2SarTestBase() {

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
    val user = cas2NomisUserEntity()

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
    val user = cas2NomisUserEntity()

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
    val user = cas2NomisUserEntity()

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
    val user = cas2NomisUserEntity()
    val assessor = cas2ExternalUserEntity()
    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2StatusUpdateEntity(application, assessment, assessor)
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
    val user = cas2NomisUserEntity()
    val assessor = cas2ExternalUserEntity()
    val application = cas2ApplicationEntity(offenderDetails, user)
    val assessment = cas2AssessmentEntity(application)

    val applicationNotes = cas2ApplicationNoteEntity(application, assessment, user)
    val statusUpdate = cas2StatusUpdateEntity(application, assessment, assessor)
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
}
