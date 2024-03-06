package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationStatusTransformer
import java.util.UUID

class StatusUpdateServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockStatusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val mockStatusUpdateDetailRepository = mockk<Cas2StatusUpdateDetailRepository>()
  private val assessor = ExternalUserEntityFactory()
    .withUsername("JOHN_SMITH_NACRO")
    .withName("John Smith")
    .withEmail("john@nacro.example.com")
    .withOrigin("NACRO")
    .produce()

  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockStatusTransformer = mockk<ApplicationStatusTransformer>()

  private val applicant = NomisUserEntityFactory().produce()
  private val application = Cas2ApplicationEntityFactory()
    .withCreatedByUser(applicant)
    .withCrn("CRN123")
    .withNomsNumber("NOMSABC")
    .produce()
  private val applicationId = application.id
  private val mockStatusFinder = mockk<Cas2PersistedApplicationStatusFinder>()
  private val applicationUrlTemplate = "http://example.com/application-status-updated/#eventId"

  private val statusUpdateService = StatusUpdateService(
    mockApplicationRepository,
    mockStatusUpdateRepository,
    mockStatusUpdateDetailRepository,
    mockDomainEventService,
    mockStatusFinder,
    mockStatusTransformer,
    applicationUrlTemplate,
  )

  val activeStatus = Cas2PersistedApplicationStatus(
    id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
    name = "activeStatusName",
    label = "",
    description = "",
    isActive = true,
  )
  private val applicationStatusUpdate = Cas2ApplicationStatusUpdate(newStatus = activeStatus.name)

  val statusDetail = Cas2PersistedApplicationStatusDetail(
    id = UUID.fromString("390e81d4-2ace-4e76-a9e3-5efa47be606e"),
    name = "exampleStatusDetail",
    label = "",
  )
  val activeStatusWithDetail = Cas2PersistedApplicationStatus(
    id = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"),
    name = "activeStatusWithDetail",
    label = "",
    description = "",
    statusDetails = listOf(
      statusDetail,
    ),
    isActive = true,
  )
  private val applicationStatusUpdateWithDetail = Cas2ApplicationStatusUpdate(
    newStatus = activeStatusWithDetail.name,
    newStatusDetails = listOf(statusDetail.name),
  )

  private val activeStatusList = listOf(activeStatus, activeStatusWithDetail)

  @BeforeEach
  fun setup() {
    every { mockStatusFinder.active() } returns activeStatusList
    every { mockStatusTransformer.transformStatusDetailListToDetailItemList(any()) } returns emptyList()
  }

  @Nested
  inner class IsValidStatus {

    @Test
    fun `returns true when the given newStatus is valid`() {
      val validUpdate = Cas2ApplicationStatusUpdate(newStatus = "activeStatusName")

      assertThat(statusUpdateService.isValidStatus(validUpdate)).isTrue()
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
          .withStatusId(activeStatus.id)
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
                statusDetails = emptyList(),
              )
            },
          )
        }
      }
    }

    @Nested
    inner class WhenUnsuccessful {

      @BeforeEach
      fun setUp() {
        every { mockApplicationRepository.findSubmittedApplicationById(applicationId) } answers
          {
            null
          }
      }

      @Test
      fun `does NOT ask the domain event service to create a status-updated event`() {
        statusUpdateService.create(
          applicationId = applicationId,
          statusUpdate = applicationStatusUpdate,
          assessor = assessor,
        )

        verify(exactly = 0) {
          mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any())
        }
      }
    }

    @Nested
    inner class WithStatusDetail {
      @Nested
      inner class WhenSuccessful {

        @BeforeEach
        fun setUp() {
          val cas2StatusUpdateEntity = Cas2StatusUpdateEntityFactory()
            .withApplication(application)
            .withAssessor(assessor)
            .withStatusId(activeStatusWithDetail.id)
            .produce()

          val cas2StatusUpdateDetailEntity = Cas2StatusUpdateDetailEntity(
            id = UUID.randomUUID(),
            statusUpdate = cas2StatusUpdateEntity,
            statusDetailId = statusDetail.id,
            label = statusDetail.label,
          )

          every { mockApplicationRepository.findSubmittedApplicationById(applicationId) } answers
            {
              application
            }

          every { mockStatusUpdateRepository.save(any()) } answers
            {
              cas2StatusUpdateEntity
            }

          every { mockStatusUpdateDetailRepository.save(any()) } answers
            {
              cas2StatusUpdateDetailEntity
            }

          every { mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any()) } answers { }
        }

        @Test
        fun `saves a status update entity with detail and emits a domain event`() {
          every { mockStatusTransformer.transformStatusDetailListToDetailItemList(listOf(statusDetail)) } returns listOf(
            Cas2StatusDetail("exampleStatusDetail", ""),
          )

          statusUpdateService.create(
            applicationId = applicationId,
            statusUpdate = applicationStatusUpdateWithDetail,
            assessor = assessor,
          )

          verify {
            mockStatusUpdateRepository.save(
              match {
                it.statusId == activeStatusWithDetail.id
              },
            )
          }

          verify {
            mockStatusUpdateDetailRepository.save(
              match {
                it.statusDetailId == statusDetail.id &&
                  it.label == statusDetail.label
              },
            )
          }

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
                  name = "offerDeclined",
                  label = "Offer declined or withdrawn",
                  description = "The accommodation offered has been declined or withdrawn.",
                  statusDetails = listOf(
                    Cas2StatusDetail("exampleStatusDetail", ""),
                  ),
                )
              },
            )
          }
        }
      }

      @Nested
      inner class WhenUnsuccessful {

        @BeforeEach
        fun setUp() {
          every { mockApplicationRepository.findSubmittedApplicationById(applicationId) } answers
            {
              null
            }
        }

        @Test
        fun `does NOT ask the domain event service to create a status-updated event`() {
          statusUpdateService.create(
            applicationId = applicationId,
            statusUpdate = applicationStatusUpdate,
            assessor = assessor,
          )

          verify(exactly = 0) {
            mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any())
          }
        }
      }
    }
  }
}
