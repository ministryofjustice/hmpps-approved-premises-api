package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
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

  private val placementApplicationService = PlacementApplicationService(
    placementApplicationRepository,
    jsonSchemaService,
    userService,
    placementDateRepository,
    placementRequestService,
    userAllocator,
    emailNotificationService,
    notifyConfig,
    userAccessService,
    sendPlacementRequestNotifications = true,
  )

  @Nested
  inner class SubmitApplicationTest {
    lateinit var user: UserEntity

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { userService.getUserForRequest() } returns user
    }

    @Test
    fun `Submitting an application sends allocation and submission notification and returns successfully`() {
      val assigneeUser = UserEntityFactory().withDefaultProbationRegion().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(null)
        .withDecision(null)
        .withSubmittedAt(null)
        .withCreatedByUser(user)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java) } returns placementApplication.schemaVersion
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementDateRepository.saveAll(any<List<PlacementDateEntity>>()) } answers { emptyList() }

      val allocatedNotifyTemplateId = UUID.randomUUID().toString()
      every { notifyConfig.templates.placementRequestAllocated } answers { allocatedNotifyTemplateId }
      val placementRequestCreatedTemplateId = UUID.randomUUID().toString()
      every { notifyConfig.templates.placementRequestSubmitted } answers { placementRequestCreatedTemplateId }
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

      val result = placementApplicationService.submitApplication(
        placementApplication.id,
        "translatedDocument",
        PlacementType.releaseFollowingDecision,
        emptyList(),
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          user.email!!,
          placementRequestCreatedTemplateId,
          mapOf(
            "crn" to placementApplication.application.crn,
          ),
        )
      }

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          user.email!!,
          allocatedNotifyTemplateId,
          mapOf(
            "crn" to placementApplication.application.crn,
          ),
        )
      }
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
        placementApplication = mockk<PlacementApplicationEntity>(),
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

      val newPlacementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          duration = 12,
          expectedArrival = LocalDate.now(),
          placementApplication = mockk<PlacementApplicationEntity>(),
        ),
      )

      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { it.invocation.args[0] as PlacementApplicationEntity }
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

      val newPlacementDates = mutableListOf(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now(),
          duration = 12,
          expectedArrival = LocalDate.now(),
          placementApplication = mockk<PlacementApplicationEntity>(),
        ),
      )

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
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit

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

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          newPlacementApplication.createdByUser.email!!,
          notifyTemplateId,
          mapOf(
            "crn" to newPlacementApplication.application.crn,
          ),
        )
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

      every { userService.getUserForRequest() } returns user
    }

    @Test
    fun `it withdraws an application`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .produce()

      val templateId = UUID.randomUUID().toString()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { notifyConfig.templates.placementRequestWithdrawn } answers { templateId }
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success

      val entity = validationResult.entity

      assertThat(entity.decision).isEqualTo(PlacementApplicationDecision.WITHDRAWN_BY_PP)
    }

    @Test
    fun `it returns unauthorised if a user did not create the placement application`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
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

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `it does not allow placement applications to be withdrawn if a decision has been made`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withCreatedByUser(user)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = placementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue()
      (validationResult as ValidatableActionResult.GeneralValidationError).let {
        assertThat(validationResult.message).isEqualTo("The Placement Application cannot be withdrawn because it has an associated decision")
      }
    }
  }
}
