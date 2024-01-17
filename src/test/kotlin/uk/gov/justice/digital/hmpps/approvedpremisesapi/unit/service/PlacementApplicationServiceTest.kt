package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationServiceTest {
  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val jsonSchemaService = mockk<JsonSchemaService>()
  private val userService = mockk<UserService>()
  private val placementDateRepository = mockk<PlacementDateRepository>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val emailNotificationService = mockk<EmailNotificationService>()
  private val userAllocator = mockk<UserAllocator>()
  private val notifyConfig = mockk<NotifyConfig>()

  private val placementApplicationService = PlacementApplicationService(
    placementApplicationRepository,
    jsonSchemaService,
    userService,
    placementDateRepository,
    placementRequestService,
    placementRequestRepository,
    emailNotificationService,
    userAllocator,
    notifyConfig,
  )

  @Nested
  inner class ReallocateApplicationTest {
    private val previousUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    private val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    private val application = ApprovedPremisesApplicationEntityFactory()
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
    fun `Reallocating an application returns successfully`() {
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
    fun `it withdraws an application and associated placement requests, emailing allocated users`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withCreatedByUser(user)
        .produce()

      val placementRequestAllocatedUser1 = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail("user@example.com")
        .produce()

      val placementRequestAllocatedUser2 = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      placementApplication.placementRequests = mutableListOf(
        createPlacementRequestForApplication(placementApplication, placementRequestAllocatedUser1),
        createPlacementRequestForApplication(placementApplication, placementRequestAllocatedUser2),
      )

      val templateId = UUID.randomUUID().toString()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { notifyConfig.templates.placementRequestWithdrawn } answers { templateId }
      every { emailNotificationService.sendEmail(any(), any(), any()) } returns Unit
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      val result = placementApplicationService.withdrawPlacementApplication(placementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success

      val entity = validationResult.entity

      assertThat(entity.decision).isEqualTo(PlacementApplicationDecision.WITHDRAWN_BY_PP)

      verify(exactly = 1) {
        emailNotificationService.sendEmail(
          placementRequestAllocatedUser1.email!!,
          templateId,
          mapOf(
            "name" to placementRequestAllocatedUser1.name,
            "crn" to placementApplication.application.crn,
          ),
        )
      }
    }

    @Test
    fun `it returns unauthorised if a user did not create the placement application`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
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

      val result = placementApplicationService.withdrawPlacementApplication(placementApplication.id)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `it does not allow placement applications with bookings to be withdrawn`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withCreatedByUser(user)
        .produce()

      val placementRequest = createPlacementRequestForApplication(placementApplication, user)

      val premisesEntity = ApprovedPremisesEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      placementRequest.booking = BookingEntityFactory()
        .withYieldedPremises { premisesEntity }
        .produce()

      placementApplication.placementRequests = mutableListOf(
        placementRequest,
      )

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = placementApplicationService.withdrawPlacementApplication(placementApplication.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue()
    }

    private fun createPlacementRequestForApplication(placementApplication: PlacementApplicationEntity, allocatedUser: UserEntity): PlacementRequestEntity {
      return PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(placementApplication.application)
            .withAssessment(placementApplication.application.assessments[0])
            .produce(),
        )
        .withApplication(placementApplication.application)
        .withPlacementApplication(placementApplication)
        .withAssessment(placementApplication.application.assessments[0])
        .withAllocatedToUser(allocatedUser)
        .produce()
    }
  }
}
