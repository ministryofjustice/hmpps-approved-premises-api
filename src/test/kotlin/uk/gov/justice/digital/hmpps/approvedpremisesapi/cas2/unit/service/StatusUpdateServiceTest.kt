package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.sentry.Sentry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class StatusUpdateServiceTest {
  private val mockStatusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val mockStatusUpdateDetailRepository = mockk<Cas2StatusUpdateDetailRepository>()
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()
  private val assessor = Cas2UserEntityFactory()
    .withUsername("JOHN_SMITH_NACRO")
    .withName("John Smith")
    .withEmail("john@nacro.example.com")
    .withExternalType("NACRO")
    .produce()

  private val mockDomainEventService = mockk<Cas2DomainEventService>()
  private val mockStatusTransformer = mockk<ApplicationStatusTransformer>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val cas2EmailService = mockk<Cas2EmailService>()

  private val applicant = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
  private val application = Cas2ApplicationEntityFactory()
    .withCreatedByUser(applicant)
    .withCrn("CRN123")
    .withNomsNumber("NOMSABC")
    .produce()
  private val applicationId = application.id
  val assessment = Cas2AssessmentEntityFactory().withApplication(application).produce()
  private val mockStatusFinder = mockk<Cas2PersistedApplicationStatusFinder>()
  private val applicationUrlTemplate = "http://example.com/application-status-updated/#eventId"
  private val applicationOverviewUrlTemplate = "http://example.com/application/#id/overview"

  private val statusUpdateService = StatusUpdateService(
    mockAssessmentRepository,
    mockStatusUpdateRepository,
    mockStatusUpdateDetailRepository,
    mockDomainEventService,
    mockEmailNotificationService,
    mockStatusFinder,
    mockStatusTransformer,
    cas2EmailService,
    applicationUrlTemplate,
    applicationOverviewUrlTemplate,
  )

  val activeStatus = Cas2PersistedApplicationStatus(
    id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
    name = "activeStatusName",
    label = "",
    description = "",
    isActive = true,
  )
  private val applicationStatusUpdate = Cas2AssessmentStatusUpdate(newStatus = activeStatus.name)

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
  private val applicationStatusUpdateWithDetail = Cas2AssessmentStatusUpdate(
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
      val validUpdate = Cas2AssessmentStatusUpdate(newStatus = "activeStatusName")

      assertThat(statusUpdateService.isValidStatus(validUpdate)).isTrue()
    }

    @Test
    fun `returns false when the given newStatus is NOT valid`() {
      val invalidUpdate = Cas2AssessmentStatusUpdate(newStatus = "invalidStatus")

      assertThat(statusUpdateService.isValidStatus(invalidUpdate)).isFalse()
    }
  }

  @Nested
  inner class CreateForAssessment {

    @Nested
    inner class WhenSuccessful {

      @BeforeEach
      fun setUp() {
        val cas2StatusUpdateEntity = Cas2StatusUpdateEntityFactory()
          .withApplication(application)
          .withAssessor(assessor)
          .withStatusId(activeStatus.id)
          .produce()

        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } answers
          {
            assessment
          }

        every { mockStatusUpdateRepository.save(any()) } answers
          {
            cas2StatusUpdateEntity
          }

        every { mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any()) } answers { }

        every {
          mockEmailNotificationService.sendCas2Email(
            recipientEmailAddress = applicant.email!!,
            templateId = Cas2NotifyTemplates.cas2ApplicationStatusUpdated,
            personalisation = mapOf(
              "applicationStatus" to cas2StatusUpdateEntity.label,
              "dateStatusChanged" to cas2StatusUpdateEntity.createdAt.toLocalDate().toCas2UiFormat(),
              "timeStatusChanged" to cas2StatusUpdateEntity.createdAt.toCas2UiFormattedHourOfDay(),
              "nomsNumber" to "NOMSABC",
              "applicationType" to "Home Detention Curfew (HDC)",
              "applicationUrl" to "http://example.com/application/$applicationId/overview",
            ),
          )
        } just Runs
      }

      @Test
      fun `saves and asks the domain event service to create a status-updated event`() {
        every { cas2EmailService.getReferrerEmail(any()) }.returns(application.createdByUser!!.email)
        statusUpdateService.createForAssessment(
          assessmentId = assessment.id,
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
                  description = "The referrer must provide information requested for the application to progress.",
                  statusDetails = emptyList(),
                )
            },
          )
        }

        verify {
          mockStatusUpdateRepository.save(
            match {
              it.assessment!!.id == assessment.id
            },
          )
        }
      }
    }

    @Nested
    inner class WhenUnsuccessful {

      @BeforeEach
      fun setUp() {
        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } answers
          {
            null
          }
      }

      @Test
      fun `does NOT ask the domain event service to create a status-updated event`() {
        statusUpdateService.createForAssessment(
          assessmentId = assessment.id,
          statusUpdate = applicationStatusUpdate,
          assessor = assessor,
        )

        verify(exactly = 0) {
          mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any())
        }

        verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
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
            .withAssessment(assessment)
            .withAssessor(assessor)
            .withStatusId(activeStatusWithDetail.id)
            .produce()

          val cas2StatusUpdateDetailEntity = Cas2StatusUpdateDetailEntity(
            id = UUID.randomUUID(),
            statusUpdate = cas2StatusUpdateEntity,
            statusDetailId = statusDetail.id,
            label = statusDetail.label,
          )

          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } answers
            {
              assessment
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

          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = applicant.email!!,
              templateId = Cas2NotifyTemplates.cas2ApplicationStatusUpdated,
              personalisation = mapOf(
                "applicationStatus" to cas2StatusUpdateEntity.label,
                "dateStatusChanged" to cas2StatusUpdateEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeStatusChanged" to cas2StatusUpdateEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "applicationType" to "Home Detention Curfew (HDC)",
                "nomsNumber" to "NOMSABC",
                "applicationUrl" to "http://example.com/application/$applicationId/overview",
              ),
            )
          } just Runs
        }

        @Test
        fun `saves a status update entity with detail and emits a domain event`() {
          every { mockStatusTransformer.transformStatusDetailListToDetailItemList(listOf(statusDetail)) } returns listOf(
            Cas2StatusDetail("exampleStatusDetail", ""),
          )
          every { cas2EmailService.getReferrerEmail(any()) } returns assessment.application.createdByUser!!.email

          statusUpdateService.createForAssessment(
            assessmentId = assessment.id,
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

        @Test
        fun `alerts Sentry when the Referrer does not have an email`() {
          val submittedApplicationWithNoReferrerEmail = Cas2ApplicationEntityFactory()
            .withCreatedByUser(Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withEmail(null).produce())
            .withCrn("CRN123")
            .withNomsNumber("NOMSABC")
            .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
            .produce()

          val assessmentWithNoEmail = Cas2AssessmentEntityFactory()
            .withApplication(submittedApplicationWithNoReferrerEmail).produce()

          every { mockAssessmentRepository.findByIdOrNull(assessmentWithNoEmail.id) } answers
            {
              assessmentWithNoEmail
            }

          every { cas2EmailService.getReferrerEmail(any()) } answers { callOriginal() }

          mockkStatic(Sentry::class)

          statusUpdateService.createForAssessment(
            assessmentId = assessmentWithNoEmail.id,
            statusUpdate = applicationStatusUpdateWithDetail,
            assessor = assessor,
          )

          verify(exactly = 1) {
            Sentry.captureMessage(
              any(),
            )
          }
        }
      }

      @Nested
      inner class WhenUnsuccessful {

        @BeforeEach
        fun setUp() {
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } answers
            {
              null
            }
        }

        @Test
        fun `does NOT ask the domain event service to create a status-updated event`() {
          statusUpdateService.createForAssessment(
            assessmentId = assessment.id,
            statusUpdate = applicationStatusUpdate,
            assessor = assessor,
          )

          verify(exactly = 0) {
            mockDomainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(any())
          }

          verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
        }
      }
    }
  }
}
