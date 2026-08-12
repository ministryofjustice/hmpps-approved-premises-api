package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration.sar

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar.Cas2SarTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcSubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

class Cas2HdcSubjectAccessRequestServiceTest : Cas2SarTestBase() {

  @Autowired
  lateinit var cas2HdcSubjectAccessRequestService: Cas2HdcSubjectAccessRequestService

  @Test
  fun `Get CAS2 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      cas2HdcSubjectAccessRequestService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNull(result)
  }

  @Test
  fun `Get CAS2 Information - null date Check`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      cas2HdcSubjectAccessRequestService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)

    assertNull(result)
  }

  @Test
  fun `Get CAS2 Information - Applications`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = cas2NomisUserEntity()

    val application = cas2ApplicationEntity(offenderDetails, user)

    val result = cas2HdcSubjectAccessRequestService.getSarResult(
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
      "DomainEvents":  []
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

    val result = cas2HdcSubjectAccessRequestService.getSarResult(
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
      "DomainEvents":  []
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

    val result = cas2HdcSubjectAccessRequestService.getSarResult(
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
      "DomainEvents":  []

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

    val result = cas2HdcSubjectAccessRequestService.getSarResult(
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
      "DomainEvents":  []
      
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
    val domainEvent = domainEventEntity(offenderDetails, application.id, assessment.id, null, DomainEventType.CAS2_APPLICATION_SUBMITTED, ServiceName.cas2)

    val result = cas2HdcSubjectAccessRequestService.getSarResult(
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
      "DomainEvents": [${domainEventJson(domainEvent, null)}]
   }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }
}
