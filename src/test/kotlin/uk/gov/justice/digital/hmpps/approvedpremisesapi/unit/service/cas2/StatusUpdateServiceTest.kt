package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService

class StatusUpdateServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockStatusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val status = Cas2ApplicationStatusSeeding.statusList().find { it.name == "moreInfoRequested" }
  private val applicationStatusUpdate = Cas2ApplicationStatusUpdate(newStatus = status!!.name)
  private val assessor = ExternalUserEntityFactory()
    .withUsername("JOHN_SMITH_NACRO")
    .withName("John Smith")
    .withEmail("john@nacro.example.com")
    .withOrigin("NACRO")
    .produce()

  private val mockDomainEventService = mockk<DomainEventService>()

  private val applicant = NomisUserEntityFactory().produce()
  private val application = Cas2ApplicationEntityFactory()
    .withCreatedByUser(applicant)
    .withCrn("CRN123")
    .withNomsNumber("NOMSABC")
    .produce()
  private val applicationId = application.id
  private val applicationUrlTemplate = "http://example.com/application-status-updated/#eventId"

  private val statusUpdateService = StatusUpdateService(
    mockApplicationRepository,
    mockStatusUpdateRepository,
    mockDomainEventService,
    applicationUrlTemplate,
  )

  @Nested
  inner class IsValidStatus {

    @Test
    fun `returns true when the given newStatus is valid`() {
      Cas2ApplicationStatusSeeding.statusList().forEach { status ->
        val validUpdate = Cas2ApplicationStatusUpdate(newStatus = status.name)
        assertThat(statusUpdateService.isValidStatus(validUpdate)).isTrue()
      }
    }

    @Test
    fun `returns false when the given newStatus is NOT valid`() {
      val invalidUpdate = Cas2ApplicationStatusUpdate(newStatus = "invalidStatus")

      assertThat(statusUpdateService.isValidStatus(invalidUpdate)).isFalse()
    }
  }

  @Nested
  inner class Create {

    @Nested
    inner class WhenSuccessful {

      @BeforeEach
      fun setUp() {
        val cas2StatusUpdateEntity = Cas2StatusUpdateEntityFactory()
          .withApplication(application)
          .withAssessor(assessor)
          .withStatusId(status!!.id)
          .produce()

        every { mockApplicationRepository.findSubmittedApplicationById(applicationId) } answers
          {
            application
          }

        every { mockStatusUpdateRepository.save(any()) } answers
          {
            cas2StatusUpdateEntity
          }

        every { mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any()) } answers { }
      }

      @Test
      fun `asks the domain event service to create a status-updated event`() {
        statusUpdateService.create(
          applicationId = applicationId,
          statusUpdate = applicationStatusUpdate,
          assessor = assessor,
        )

        verify {
          mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(
            match {
              it.crn == "CRN123" &&
                it.applicationId == applicationId &&
                it.data.eventType == EventType.applicationStatusUpdated &&
                it.data.eventDetails.applicationId == applicationId &&
                it.data.eventDetails.applicationUrl == "http://example.com/application-status-updated/#eventId" &&
                it.data.eventDetails.updatedBy == ExternalUser(
                username = "JOHN_SMITH_NACRO",
                name = "John Smith",
                email = "john@nacro.example.com",
                origin = "NACRO",
              ) &&
                it.data.eventDetails.personReference == PersonReference(
                crn = "CRN123",
                noms = "NOMSABC",
              ) &&
                it.data.eventDetails.newStatus == Cas2Status(
                name = "moreInfoRequested",
                label = "More information requested",
                description = "The prison offender manager (POM) must provide information requested for the application to progress.",
              )
            },
          )
        }
      }
    }

    @Nested
    inner class WhenUnsuccessful {
      @Test
      fun `does NOT ask the domain event service to create a status-updated event`() {
        org.junit.jupiter.api.fail("not yet implemented")
      }
    }
  }
}
