package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationServiceTest {
  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val jsonSchemaService = mockk<JsonSchemaService>()
  private val userService = mockk<UserService>()
  private val placementDateRepository = mockk<PlacementDateRepository>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val emailNotificationService = mockk<EmailNotificationService>()
  private val userAllocator = mockk<UserAllocator>()
  private val notifyConfig = mockk<NotifyConfig>()
  private val userAccessService = mockk<UserAccessService>()
  private val cas1PlacementApplicationEmailService = mockk<Cas1PlacementApplicationEmailService>()
  private val cas1PlacementApplicationDomainEventService = mockk<Cas1PlacementApplicationDomainEventService>()
  private val taskDeadlineServiceMock = mockk<TaskDeadlineService>()

  private val placementApplicationService = buildService(useNewWithdrawalLogic = true)
  private val placementApplicationServiceLegacyWithdrawalLogic = buildService(useNewWithdrawalLogic = false)

  private fun buildService(useNewWithdrawalLogic: Boolean) = PlacementApplicationService(
    placementApplicationRepository,
    jsonSchemaService,
    userService,
    placementDateRepository,
    placementRequestService,
    userAllocator,
    emailNotificationService,
    notifyConfig,
    userAccessService,
    cas1PlacementApplicationEmailService,
    cas1PlacementApplicationDomainEventService,
    sendPlacementRequestNotifications = true,
    taskDeadlineServiceMock,
    useNewWithdrawalLogic = useNewWithdrawalLogic,
  )

  @Nested
  inner class CreatePlacementApplicationTest {
    lateinit var user: UserEntity

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { userService.getUserForRequest() } returns user
    }

    @Test
    fun `If application withdrawn, return error`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withStatus(ApprovedPremisesApplicationStatus.WITHDRAWN)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withDecision(AssessmentDecision.ACCEPTED)
        .produce()

      application.assessments = mutableListOf(
        assessment,
      )

      val result = placementApplicationService.createPlacementApplication(application, user)

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message)
        .isEqualTo("You cannot request a placement request for an application that has been withdrawn")
    }
  }

  @Nested
  inner class SubmitApplicationTest {
    lateinit var user: UserEntity
    lateinit var dueAt: OffsetDateTime
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementApplication: PlacementApplicationEntity

    private val assigneeUser = UserEntityFactory().withDefaultProbationRegion().produce()

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      dueAt = OffsetDateTime.now()

      application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(null)
        .withDecision(null)
        .withSubmittedAt(null)
        .withCreatedByUser(user)
        .produce()

      every { userService.getUserForRequest() } returns user
      every { taskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
    }

    @Test
    fun `Submitting an application triggers allocation and sets a due date`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java) } returns placementApplication.schemaVersion
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementDateRepository.saveAll(any<List<PlacementDateEntity>>()) } answers { emptyList() }

      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } returns Unit

      val result = placementApplicationServiceLegacyWithdrawalLogic.submitApplication(
        placementApplication.id,
        "translatedDocument",
        PlacementType.releaseFollowingDecision,
        emptyList(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue

      val validatableActionResult = (result as AuthorisableActionResult.Success).entity
      val updatedApplication = (validatableActionResult as ValidatableActionResult.Success).entity

      assertThat(updatedApplication[0].dueAt).isEqualTo(dueAt)
    }

    @Test
    fun `Submitting an application saves a single date to a placement application and triggers emails`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java) } returns placementApplication.schemaVersion
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementDateRepository.save(any()) } answers { it.invocation.args[0] as PlacementDateEntity }

      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } returns Unit

      val result = placementApplicationService.submitApplication(
        placementApplication.id,
        "translatedDocument",
        PlacementType.releaseFollowingDecision,
        listOf(
          PlacementDates(expectedArrival = LocalDate.of(2024, 4, 1), duration = 5),
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromNestedAuthorisableValidatableActionResult(result)

      assertThat(updatedPlacementApplications).hasSize(1)

      val updatedPlacementApp = updatedPlacementApplications[0]
      assertThat(updatedPlacementApp.submissionGroupId).isNotNull()

      assertThat(updatedPlacementApp.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(updatedPlacementApp.placementDates[0].duration).isEqualTo(5)

      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp) }
    }

    @Test
    fun `Submitting an application saves multiple dates to individual placement applications and triggers emails per resultant placement application`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java) } returns placementApplication.schemaVersion
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementDateRepository.save(any()) } answers { it.invocation.args[0] as PlacementDateEntity }

      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(any()) } returns Unit

      val result = placementApplicationService.submitApplication(
        placementApplication.id,
        "translatedDocument",
        PlacementType.releaseFollowingDecision,
        listOf(
          PlacementDates(expectedArrival = LocalDate.of(2024, 4, 1), duration = 5),
          PlacementDates(expectedArrival = LocalDate.of(2024, 5, 2), duration = 10),
          PlacementDates(expectedArrival = LocalDate.of(2024, 6, 3), duration = 15),
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromNestedAuthorisableValidatableActionResult(result)

      assertThat(updatedPlacementApplications).hasSize(3)

      val firstSubmissionGroupId = updatedPlacementApplications[0].submissionGroupId

      val updatedPlacementApp1 = updatedPlacementApplications[0]
      assertThat(updatedPlacementApp1.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(updatedPlacementApp1.placementDates[0].duration).isEqualTo(5)
      assertThat(updatedPlacementApp1.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp1) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp1) }

      val updatedPlacementApp2 = updatedPlacementApplications[1]
      assertThat(updatedPlacementApp2.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 5, 2))
      assertThat(updatedPlacementApp2.placementDates[0].duration).isEqualTo(10)
      assertThat(updatedPlacementApp2.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp2) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp2) }

      val updatedPlacementApp3 = updatedPlacementApplications[2]
      assertThat(updatedPlacementApp3.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 6, 3))
      assertThat(updatedPlacementApp3.placementDates[0].duration).isEqualTo(15)
      assertThat(updatedPlacementApp3.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp3) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp3) }
    }

    @Test
    fun `Submitting an application saves multiple dates to a single placement application (legacy logic) and triggers a single set of emails`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java) } returns placementApplication.schemaVersion
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementDateRepository.saveAll(any<List<PlacementDateEntity>>()) } answers { emptyList() }

      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } returns Unit

      val result = placementApplicationServiceLegacyWithdrawalLogic.submitApplication(
        placementApplication.id,
        "translatedDocument",
        PlacementType.releaseFollowingDecision,
        listOf(
          PlacementDates(expectedArrival = LocalDate.of(2024, 4, 1), duration = 5),
          PlacementDates(expectedArrival = LocalDate.of(2024, 5, 2), duration = 10),
          PlacementDates(expectedArrival = LocalDate.of(2024, 6, 3), duration = 15),
        ),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromNestedAuthorisableValidatableActionResult(result)

      assertThat(updatedPlacementApplications).hasSize(1)

      val updatedPlacementApp = updatedPlacementApplications[0]
      assertThat(updatedPlacementApp.submissionGroupId).isNotNull()

      val dates = updatedPlacementApp.placementDates
      assertThat(dates).hasSize(3)
      assertThat(dates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(dates[0].duration).isEqualTo(5)
      assertThat(dates[1].expectedArrival).isEqualTo(LocalDate.of(2024, 5, 2))
      assertThat(dates[1].duration).isEqualTo(10)
      assertThat(dates[2].expectedArrival).isEqualTo(LocalDate.of(2024, 6, 3))
      assertThat(dates[2].duration).isEqualTo(15)

      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp) }
    }
  }

  @Nested
  inner class DecisionTest {
    lateinit var user: UserEntity
    lateinit var createdByUser: UserEntity

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { userService.getUserForRequest() } returns user

      createdByUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
    }

    @Test
    fun `Submitting an accepted application decision sends a notification and returns successfully`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every {
        placementRequestService.createPlacementRequestsFromPlacementApplication(any(), any())
      } returns AuthorisableActionResult.Success(emptyList())
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      val notifyAcceptedTemplateId = UUID.randomUUID().toString()
      every { notifyConfig.templates.placementRequestDecisionAccepted } answers { notifyAcceptedTemplateId }
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

      val result = placementApplicationService.recordDecision(
        placementApplication.id,
        PlacementApplicationDecisionEnvelope(
          decision = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.accepted,
          summaryOfChanges = "summaryOfChanges",
          decisionSummary = "decisionSummary",
        ),
      )

      val validationResult = (result as AuthorisableActionResult.Success).entity
      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success
      val updatedApplication = validationResult.entity

      assertThat(updatedApplication.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
      assertThat(updatedApplication.decisionMadeAt).isWithinTheLastMinute()

      verify { placementRequestService.createPlacementRequestsFromPlacementApplication(placementApplication, "decisionSummary") }

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          createdByUser.email!!,
          notifyAcceptedTemplateId,
          mapOf(
            "crn" to application.crn,
          ),
        )
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision::class,
      names = ["accepted"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Submitting rejected decisions sends a notification and returns successfully`(decision: uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      val notifyRejectedTemplateId = UUID.randomUUID().toString()
      every { notifyConfig.templates.placementRequestDecisionRejected } answers { notifyRejectedTemplateId }
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

      val result = placementApplicationService.recordDecision(
        placementApplication.id,
        PlacementApplicationDecisionEnvelope(
          decision = decision,
          summaryOfChanges = "summaryOfChanges",
          decisionSummary = "decisionSummary",
        ),
      )

      val validationResult = (result as AuthorisableActionResult.Success).entity
      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success
      val updatedApplication = validationResult.entity

      val expectedDecision = when (decision) {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.accepted -> PlacementApplicationDecision.ACCEPTED
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.rejected -> PlacementApplicationDecision.REJECTED
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.withdraw -> PlacementApplicationDecision.WITHDRAW
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.withdrawnByPp -> PlacementApplicationDecision.WITHDRAWN_BY_PP
      }

      assertThat(updatedApplication.decision).isEqualTo(expectedDecision)
      assertThat(updatedApplication.decisionMadeAt).isWithinTheLastMinute()

      verify { placementRequestService wasNot Called }

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          createdByUser.email!!,
          notifyRejectedTemplateId,
          mapOf(
            "crn" to application.crn,
          ),
        )
      }
    }
  }

  @Nested
  inner class ReallocateApplicationTest {
    private val assigneeUser = UserEntityFactory().withDefaultProbationRegion().produce()

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
      .produce()

    private val previousPlacementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .withDecision(null)
      .withCreatedByUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .produce()

    private val placementDates = mutableListOf(
      PlacementDateEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        duration = 12,
        expectedArrival = LocalDate.now(),
        placementApplication = previousPlacementApplication,
      ),
    )

    @Test
    fun `Reallocating an allocated application returns successfully`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

      previousPlacementApplication.placementDates = placementDates

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .withDecision(null)
        .withCreatedByUser(assigneeUser)
        .produce()

      val newPlacementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          duration = 12,
          expectedArrival = LocalDate.now(),
          placementApplication = placementApplication,
        ),
      )

      val dueAt = OffsetDateTime.now()

      every { taskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { previousPlacementApplication }
      every { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every {
        placementDateRepository.saveAll<PlacementDateEntity>(
          match { it.first().expectedArrival == placementDates[0].expectedArrival && it.first().duration == placementDates[0].duration },
        )
      } answers { newPlacementDates }

      val result = placementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success

      assertThat(previousPlacementApplication.reallocatedAt).isNotNull

      verify { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) }

      val newPlacementApplication = validationResult.entity

      assertThat(newPlacementApplication.application).isEqualTo(application)
      assertThat(newPlacementApplication.allocatedToUser).isEqualTo(assigneeUser)
      assertThat(newPlacementApplication.createdByUser).isEqualTo(previousPlacementApplication.createdByUser)
      assertThat(newPlacementApplication.data).isEqualTo(previousPlacementApplication.data)
      assertThat(newPlacementApplication.document).isEqualTo(previousPlacementApplication.document)
      assertThat(newPlacementApplication.schemaVersion).isEqualTo(previousPlacementApplication.schemaVersion)
      assertThat(newPlacementApplication.placementType).isEqualTo(previousPlacementApplication.placementType)
      assertThat(newPlacementApplication.placementDates).isEqualTo(newPlacementDates)
      assertThat(newPlacementApplication.dueAt).isEqualTo(dueAt)
    }

    @Test
    fun `Reallocating an unallocated application sends a notification and returns successfully`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

      previousPlacementApplication.placementDates = placementDates
      previousPlacementApplication.allocatedToUser = null

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .withDecision(null)
        .withCreatedByUser(assigneeUser)
        .produce()

      val newPlacementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          duration = 12,
          expectedArrival = LocalDate.now(),
          placementApplication = placementApplication,
        ),
      )

      val dueAt = OffsetDateTime.now()

      every { taskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every {
        placementDateRepository.saveAll<PlacementDateEntity>(
          match { it.first().expectedArrival == placementDates[0].expectedArrival && it.first().duration == placementDates[0].duration },
        )
      } answers { newPlacementDates }

      val notifyTemplateId = UUID.randomUUID().toString()
      every { notifyConfig.templates.placementRequestAllocated } answers { notifyTemplateId }
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(any()) } returns Unit

      val result = placementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success

      assertThat(previousPlacementApplication.reallocatedAt).isNotNull

      verify { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) }

      val newPlacementApplication = validationResult.entity

      assertThat(newPlacementApplication.application).isEqualTo(application)
      assertThat(newPlacementApplication.allocatedToUser).isEqualTo(assigneeUser)
      assertThat(newPlacementApplication.createdByUser).isEqualTo(previousPlacementApplication.createdByUser)
      assertThat(newPlacementApplication.data).isEqualTo(previousPlacementApplication.data)
      assertThat(newPlacementApplication.document).isEqualTo(previousPlacementApplication.document)
      assertThat(newPlacementApplication.schemaVersion).isEqualTo(previousPlacementApplication.schemaVersion)
      assertThat(newPlacementApplication.placementType).isEqualTo(previousPlacementApplication.placementType)
      assertThat(newPlacementApplication.placementDates).isEqualTo(newPlacementDates)
      assertThat(newPlacementApplication.dueAt).isEqualTo(dueAt)

      verify(exactly = 1) {
        cas1PlacementApplicationEmailService.placementApplicationAllocated(newPlacementApplication)
      }
    }

    @Test
    fun `Reallocating a placement application that doesnt exist returns not found`() {
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns null

      val result = placementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `Reallocating a placement application with a decision returns a General Validation Error`() {
      previousPlacementApplication.apply {
        decision = PlacementApplicationDecision.ACCEPTED
      }

      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      val result = placementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue
      validationResult as ValidatableActionResult.GeneralValidationError
      assertThat(validationResult.message).isEqualTo("This placement application has already been completed")
    }

    @Test
    fun `Reallocating a placement application when user to assign to is not a MATCHER returns a field validation error`() {
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      val result = placementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
      validationResult as ValidatableActionResult.FieldValidationError
      assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingMatcherRole")
    }
  }

  @Nested
  inner class GetWithdrawableState {

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    @Test
    fun `getWithdrawableState not withdrawable if not submitted`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withDecision(null)
        .withReallocatedAt(null)
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns true

      val result = placementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState not withdrawable if already withdrawn`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(PlacementApplicationDecision.WITHDRAW)
        .withReallocatedAt(null)
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns true

      val result = placementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState not withdrawable if reallocated`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns true

      val result = placementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState withdrawable if is submitted, not reallocated and not withdrawn`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withReallocatedAt(null)
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns true

      val result = placementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawable).isTrue()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns canWithdraw

      val result = placementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }
  }

  @Nested
  inner class WithdrawPlacementApplication {
    lateinit var user: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var assessment: ApprovedPremisesAssessmentEntity

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      application.assessments = mutableListOf(
        assessment,
      )
    }

    @Test
    fun `it withdraws a placement application and triggers emails and domain events and cascades to descendants`() {
      val allocatedTo = UserEntityFactory().withDefaultProbationRegion().produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(allocatedTo)
        .withDecision(null)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val withdrawalContext = WithdrawalContext(
        user,
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
      )

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(placementApplication, withdrawalContext) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(any(), any()) } returns Unit

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = withdrawalContext,
      )

      assertThat(result is CasResult.Success).isTrue
      val entity = (result as CasResult.Success).value

      assertThat(entity.decision).isEqualTo(PlacementApplicationDecision.WITHDRAW)

      verify { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(placementApplication, allocatedTo) }
    }

    @Test
    fun `if withdraw was triggered by application, set correct withdrawal reason`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(any(), any()) } returns Unit

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
          placementApplication.id,
        ),
      )

      assertThat(result is CasResult.Success).isTrue
      val entity = (result as CasResult.Success).value

      assertThat(entity.withdrawalReason).isEqualTo(PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN)
    }

    @ParameterizedTest
    @EnumSource(value = WithdrawableEntityType::class, names = ["Application", "PlacementApplication"], mode = EnumSource.Mode.EXCLUDE)
    fun `if withdraw is triggered by an entity that shouldn't cascade to placement applications, throws exception`(triggeringEntity: WithdrawableEntityType) {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      Assertions.assertThatThrownBy {
        placementApplicationService.withdrawPlacementApplication(
          placementApplication.id,
          PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
          withdrawalContext = WithdrawalContext(
            user,
            triggeringEntity,
            placementApplication.id,
          ),
        )
      }.hasMessage("Internal Server Error: Withdrawing a ${triggeringEntity.name} should not cascade to PlacementApplications")
    }

    @Test
    fun `it is idempotent, returning success if application already withdrawn`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(PlacementApplicationDecision.WITHDRAW)
        .withCreatedByUser(user)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
        ),
      )

      assertThat(result is CasResult.Success).isTrue
    }
  }
}
