package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

class Cas1PlacementApplicationServiceTest {
  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val userService = mockk<UserService>()
  private val placementRequestService = mockk<Cas1PlacementRequestService>()
  private val userAllocator = mockk<UserAllocator>()
  private val userAccessService = mockk<Cas1UserAccessService>()
  private val cas1PlacementApplicationEmailService = mockk<Cas1PlacementApplicationEmailService>()
  private val cas1PlacementApplicationDomainEventService = mockk<Cas1PlacementApplicationDomainEventService>()
  private val cas1TaskDeadlineServiceMock = mockk<Cas1TaskDeadlineService>()
  private val lockablePlacementApplicationRepository = mockk<LockablePlacementApplicationRepository>()
  private val objectMapper = mockk<ObjectMapper>()

  private val cas1PlacementApplicationService = Cas1PlacementApplicationService(
    placementApplicationRepository,
    userService,
    placementRequestService,
    userAllocator,
    userAccessService,
    cas1PlacementApplicationEmailService,
    cas1PlacementApplicationDomainEventService,
    cas1TaskDeadlineServiceMock,
    Clock.systemDefaultZone(),
    lockablePlacementApplicationRepository,
    objectMapper,
  )

  @Nested
  inner class CreatePlacementApplication {
    lateinit var user: UserEntity

    @BeforeEach
    fun setup() {
      user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { userService.getUserForRequest() } returns user
    }

    @Test
    fun success() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withDecision(AssessmentDecision.ACCEPTED)
        .produce()

      application.assessments = mutableListOf(
        assessment,
      )

      val placementApplicationCaptor = slot<PlacementApplicationEntity>()

      every { placementApplicationRepository.save(capture(placementApplicationCaptor)) } returnsArgument 0

      cas1PlacementApplicationService.createPlacementApplication(application, user)

      val persisted = placementApplicationCaptor.captured
      assertThat(persisted.application).isEqualTo(application)
      assertThat(persisted.createdAt).isWithinTheLastMinute()
      assertThat(persisted.submissionGroupId).isNotNull()
      assertThat(persisted.automatic).isFalse
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

      val result = cas1PlacementApplicationService.createPlacementApplication(application, user)

      assertThatCasResult(result).isGeneralValidationError("You cannot request a placement request for an application that has been withdrawn")
    }
  }

  @Nested
  inner class CreateAutomaticPlacementApplication {

    @Test
    fun success() {
      val placementApplicationCaptor = slot<PlacementApplicationEntity>()

      every { placementApplicationRepository.save(capture(placementApplicationCaptor)) } returnsArgument 0

      every { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(any(), createdByUserName = null) } just Runs

      val applicationCreator = UserEntityFactory().withDefaults().produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(applicationCreator)
        .withCreatedAt(OffsetDateTime.parse("2018-12-03T10:15:30+01:00"))
        .withSubmittedAt(OffsetDateTime.parse("2018-12-04T10:15:30+01:00"))
        .withDuration(25)
        .produce()

      val id = UUID.randomUUID()

      val assessor = UserEntityFactory().withDefaults().produce()

      cas1PlacementApplicationService.createAutomaticPlacementApplication(
        id = id,
        assessment = ApprovedPremisesAssessmentEntityFactory()
          .withApplication(application)
          .withSubmittedAt(OffsetDateTime.parse("2018-12-04T10:15:30+01:00"))
          .withAllocatedToUser(assessor)
          .withAllocatedAt(OffsetDateTime.parse("2018-12-05T10:15:30+01:00"))
          .produce(),
        authorisedExpectedArrival = LocalDate.parse("2029-12-11"),
        authorisedDurationDays = 27,
      )

      val persisted = placementApplicationCaptor.captured

      assertThat(persisted.id).isEqualTo(id)
      assertThat(persisted.expectedArrival).isEqualTo(LocalDate.parse("2029-12-11"))
      assertThat(persisted.requestedDuration).isEqualTo(25)
      assertThat(persisted.authorisedDuration).isEqualTo(27)
      assertThat(persisted.placementType).isEqualTo(JpaPlacementType.AUTOMATIC)
      assertThat(persisted.application).isEqualTo(application)
      assertThat(persisted.createdByUser).isEqualTo(applicationCreator)
      assertThat(persisted.createdAt).isEqualTo(OffsetDateTime.parse("2018-12-03T10:15:30+01:00"))
      assertThat(persisted.automatic).isTrue
      assertThat(persisted.data).isNull()
      assertThat(persisted.document).isNull()
      assertThat(persisted.submittedAt).isEqualTo(OffsetDateTime.parse("2018-12-04T10:15:30+01:00"))
      assertThat(persisted.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
      assertThat(persisted.decisionMadeAt).isEqualTo(OffsetDateTime.parse("2018-12-04T10:15:30+01:00"))
      assertThat(persisted.placementRequest).isNull()
      assertThat(persisted.withdrawalReason).isNull()
      assertThat(persisted.isWithdrawn).isFalse
      assertThat(persisted.submissionGroupId).isNotNull
      assertThat(persisted.dueAt).isNull()
      assertThat(persisted.allocatedToUser).isEqualTo(assessor)
      assertThat(persisted.allocatedAt).isEqualTo(OffsetDateTime.parse("2018-12-05T10:15:30+01:00"))
      assertThat(persisted.reallocatedAt).isNull()

      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(persisted, createdByUserName = null) }
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
      every { cas1TaskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
      every { objectMapper.writeValueAsString(any()) } returns "some-json-data"
    }

    @Test
    fun `Submitting an application returns validation error if no dates defined`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = PlacementType.releaseFollowingDecision,
        placementDates = emptyList(),
        requestedPlacementPeriods = null,
        releaseType = ReleaseTypeOption.licence,
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,

      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Please provide at least one of placement dates or requested placement periods.")
    }

    @Test
    fun `Submitting an application returns validation error if no placement type or release type present`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = null,
        placementDates = emptyList(),
        requestedPlacementPeriods = listOf(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 4, 1),
            duration = 5,
            arrivalFlexible = null,
          ),
        ),
        releaseType = null,
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,

      )

      assertThat(result is CasResult.GeneralValidationError).isTrue
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Please provide at least one of placementType or releaseType.")
    }

    @ParameterizedTest
    @CsvSource(
      "paroleDirectedLicence, RELEASE_FOLLOWING_DECISION",
      "rotl, ROTL",
      "licence, ADDITIONAL_PLACEMENT",
      "hdc, ADDITIONAL_PLACEMENT",
      "pss, ADDITIONAL_PLACEMENT",
      "inCommunity, ADDITIONAL_PLACEMENT",
      "notApplicable, ADDITIONAL_PLACEMENT",
      "extendedDeterminateLicence, ADDITIONAL_PLACEMENT",
      "reReleasedPostRecall, ADDITIONAL_PLACEMENT",
    )
    fun `Submitting an application and inferring placement type from release type`(releaseType: String, jpaPlacementType: String) {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      every { userService.getDeliusUserNameForRequest() } returns "theUsername"
      every { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } just Runs
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } just Runs
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), null) } just Runs

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = null,
        placementDates = emptyList(),
        requestedPlacementPeriods = listOf(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 4, 1),
            duration = 5,
            arrivalFlexible = true,
          ),
        ),
        releaseType = ReleaseTypeOption.valueOf(releaseType),
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,
      )

      assertThat(result is CasResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromCasResult(result)

      assertThat(updatedPlacementApplications).hasSize(1)

      val updatedPlacementApp = updatedPlacementApplications[0]

      assertThat(updatedPlacementApp.releaseType).isEqualTo(releaseType)
      assertThat(updatedPlacementApp.placementType.toString()).isEqualTo(jpaPlacementType)

      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(updatedPlacementApp, "theUsername") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp) }
      verify { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(updatedPlacementApp, null) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp) }
    }

    @Test
    fun `Submitting an application triggers allocation and sets a due date`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      every { userService.getDeliusUserNameForRequest() } returns "theUsername"
      every { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } just Runs
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } just Runs
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), null) } just Runs

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = PlacementType.releaseFollowingDecision,
        placementDates = emptyList(),
        requestedPlacementPeriods = listOf(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 4, 1),
            duration = 5,
            arrivalFlexible = null,
          ),
        ),
        releaseType = ReleaseTypeOption.licence,
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,
      )

      assertThat(result is CasResult.Success).isTrue

      val updatedApplication = (result as CasResult.Success).value

      assertThat(updatedApplication[0].dueAt).isEqualTo(dueAt)
    }

    @Test
    fun `Submitting an application saves a single date to a placement application, triggers emails and domain event`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      every { userService.getDeliusUserNameForRequest() } returns "theUsername"
      every { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication) } just Runs
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication) } just Runs
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), null) } just Runs

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = PlacementType.releaseFollowingDecision,
        placementDates = emptyList(),
        requestedPlacementPeriods = listOf(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 4, 1),
            duration = 5,
            arrivalFlexible = true,
          ),
        ),
        releaseType = ReleaseTypeOption.licence,
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,
      )

      assertThat(result is CasResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromCasResult(result)

      assertThat(updatedPlacementApplications).hasSize(1)

      val updatedPlacementApp = updatedPlacementApplications[0]
      assertThat(updatedPlacementApp.submissionGroupId).isNotNull()

      assertThat(updatedPlacementApp.expectedArrival).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(updatedPlacementApp.requestedDuration).isEqualTo(5)
      assertThat(updatedPlacementApp.expectedArrivalFlexible).isTrue
      assertThat(updatedPlacementApp.authorisedDuration).isNull()
      assertThat(updatedPlacementApp.releaseType).isEqualTo(ReleaseTypeOption.licence.toString())

      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(updatedPlacementApp, "theUsername") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp) }
      verify { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(updatedPlacementApp, null) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp) }
    }

    @Test
    fun `Submitting an application saves multiple dates to individual placement applications and triggers emails and domain event per resultant placement application`() {
      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { userAllocator.getUserForPlacementApplicationAllocation(placementApplication) } returns assigneeUser
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { userService.getDeliusUserNameForRequest() } returns "theUsername"
      every { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationSubmitted(any()) } just Runs
      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(any()) } just Runs
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), null) } just Runs

      val submitPlacementApplication = SubmitPlacementApplication(
        translatedDocument = "translatedDocument",
        placementType = PlacementType.releaseFollowingDecision,
        placementDates = emptyList(),
        requestedPlacementPeriods = listOf(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 4, 1),
            duration = 5,
            arrivalFlexible = false,
          ),
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 5, 2),
            duration = 10,
            arrivalFlexible = null,
          ),
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.of(2024, 6, 3),
            duration = 15,
            arrivalFlexible = true,
          ),
        ),
        releaseType = ReleaseTypeOption.licence,
        sentenceType = null,
        situationType = null,
      )

      val result = cas1PlacementApplicationService.submitApplication(
        placementApplication.id,
        submitPlacementApplication,
      )

      assertThat(result is CasResult.Success).isTrue
      val updatedPlacementApplications = extractEntityFromCasResult(result)

      assertThat(updatedPlacementApplications).hasSize(3)

      val firstSubmissionGroupId = updatedPlacementApplications[0].submissionGroupId

      val updatedPlacementApp1 = updatedPlacementApplications[0]
      assertThat(updatedPlacementApp1.expectedArrival).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(updatedPlacementApp1.requestedDuration).isEqualTo(5)
      assertThat(updatedPlacementApp1.expectedArrivalFlexible).isFalse
      assertThat(updatedPlacementApp1.authorisedDuration).isNull()
      assertThat(updatedPlacementApp1.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(updatedPlacementApp1, "theUsername") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp1) }
      verify { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(updatedPlacementApp1, null) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp1) }

      val updatedPlacementApp2 = updatedPlacementApplications[1]
      assertThat(updatedPlacementApp2.expectedArrival).isEqualTo(LocalDate.of(2024, 5, 2))
      assertThat(updatedPlacementApp2.requestedDuration).isEqualTo(10)
      assertThat(updatedPlacementApp2.expectedArrivalFlexible).isNull()
      assertThat(updatedPlacementApp2.authorisedDuration).isNull()
      assertThat(updatedPlacementApp2.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(updatedPlacementApp2, "theUsername") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp2) }
      verify { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(updatedPlacementApp2, null) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp2) }

      val updatedPlacementApp3 = updatedPlacementApplications[2]
      assertThat(updatedPlacementApp3.expectedArrival).isEqualTo(LocalDate.of(2024, 6, 3))
      assertThat(updatedPlacementApp3.requestedDuration).isEqualTo(15)
      assertThat(updatedPlacementApp3.expectedArrivalFlexible).isTrue
      assertThat(updatedPlacementApp3.authorisedDuration).isNull()
      assertThat(updatedPlacementApp3.submissionGroupId).isEqualTo(firstSubmissionGroupId)
      verify { cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(updatedPlacementApp3, "theUsername") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAllocated(updatedPlacementApp3) }
      verify { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(updatedPlacementApp3, null) }
      verify { cas1PlacementApplicationEmailService.placementApplicationSubmitted(updatedPlacementApp3) }
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
    fun `Accepting sends a notification and returns successfully`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .withRequestedDuration(7)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelope(
        decision = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.accepted,
        summaryOfChanges = "summaryOfChanges",
        decisionSummary = "decisionSummary accepted",
      )

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every {
        placementRequestService.createPlacementRequestsFromPlacementApplication(any(), any())
      } returns CasResult.Success(Unit)
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      every { cas1PlacementApplicationEmailService.placementApplicationAccepted(any()) } returns Unit
      every { cas1PlacementApplicationDomainEventService.placementApplicationAssessed(any(), any(), any()) } returns Unit

      val result = cas1PlacementApplicationService.recordDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
      )

      assertThat(result is CasResult.Success).isTrue
      val updatedApplication = (result as CasResult.Success).value

      assertThat(updatedApplication.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
      assertThat(updatedApplication.decisionMadeAt).isWithinTheLastMinute()
      assertThat(updatedApplication.authorisedDuration).isEqualTo(7)

      verify { placementRequestService.createPlacementRequestsFromPlacementApplication(placementApplication, "decisionSummary accepted") }
      verify { cas1PlacementApplicationEmailService.placementApplicationAccepted(placementApplication) }
      verify {
        cas1PlacementApplicationDomainEventService.placementApplicationAssessed(
          match { it.id == placementApplication.id },
          user,
          placementApplicationDecisionEnvelope,
        )
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision::class,
      names = ["accepted"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Rejecting sends a notification and returns successfully`(decision: uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelope(
        decision = decision,
        summaryOfChanges = "summaryOfChanges",
        decisionSummary = "decisionSummary rejected",
      )

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { cas1PlacementApplicationEmailService.placementApplicationRejected(any()) } returns Unit
      every { cas1PlacementApplicationDomainEventService.placementApplicationAssessed(any(), any(), any()) } returns Unit

      val result = cas1PlacementApplicationService.recordDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
      )

      assertThat(result is CasResult.Success).isTrue
      val updatedApplication = (result as CasResult.Success).value

      val expectedDecision = when (decision) {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.accepted -> PlacementApplicationDecision.ACCEPTED
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.rejected -> PlacementApplicationDecision.REJECTED
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.withdraw -> PlacementApplicationDecision.WITHDRAW
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision.withdrawnByPp -> PlacementApplicationDecision.WITHDRAWN_BY_PP
      }

      assertThat(updatedApplication.decision).isEqualTo(expectedDecision)
      assertThat(updatedApplication.decisionMadeAt).isWithinTheLastMinute()

      verify { placementRequestService wasNot Called }
      verify { cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplication) }
      verify {
        cas1PlacementApplicationDomainEventService.placementApplicationAssessed(
          match { it.id == placementApplication.id },
          user,
          placementApplicationDecisionEnvelope,
        )
      }
    }
  }

  @Nested
  inner class ReallocateApplicationTest {
    private val currentRequestUser = UserEntityFactory().withDefaultProbationRegion().produce()
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
      .withSubmittedAt(OffsetDateTime.now())
      .withExpectedArrival(LocalDate.now())
      .withRequestedDuration(12)
      .produce()

    @Test
    fun `Reallocating an allocated application returns successfully`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

      val dueAt = OffsetDateTime.now()

      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { cas1TaskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { previousPlacementApplication }
      every { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { userService.getUserForRequest() } returns currentRequestUser
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), currentRequestUser) } just Runs

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isSuccess().with { newPlacementApplication ->

        assertThat(previousPlacementApplication.reallocatedAt).isNotNull

        verify { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) }
        verify {
          cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
            match { it.allocatedToUser == assigneeUser },
            currentRequestUser,
          )
        }

        assertThat(newPlacementApplication.application).isEqualTo(application)
        assertThat(newPlacementApplication.allocatedToUser).isEqualTo(assigneeUser)
        assertThat(newPlacementApplication.createdByUser).isEqualTo(previousPlacementApplication.createdByUser)
        assertThat(newPlacementApplication.data).isEqualTo(previousPlacementApplication.data)
        assertThat(newPlacementApplication.document).isEqualTo(previousPlacementApplication.document)
        assertThat(newPlacementApplication.placementType).isEqualTo(previousPlacementApplication.placementType)
        assertThat(newPlacementApplication.dueAt).isEqualTo(dueAt)
        assertThat(newPlacementApplication.expectedArrival).isEqualTo(LocalDate.now())
        assertThat(newPlacementApplication.requestedDuration).isEqualTo(12)
      }
    }

    @Test
    fun `Reallocating an unallocated application sends a notification and returns successfully`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

      previousPlacementApplication.allocatedToUser = null

      val dueAt = OffsetDateTime.now()

      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { cas1TaskDeadlineServiceMock.getDeadline(any<PlacementApplicationEntity>()) } returns dueAt
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      every { userService.getUserForRequest() } returns currentRequestUser

      every { cas1PlacementApplicationEmailService.placementApplicationAllocated(any()) } just Runs
      every { cas1PlacementApplicationDomainEventService.placementApplicationAllocated(any(), currentRequestUser) } just Runs

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isSuccess().with { newPlacementApplication ->

        assertThat(previousPlacementApplication.reallocatedAt).isNotNull

        verify { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) }
        verify {
          cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
            match { it.allocatedToUser == assigneeUser },
            currentRequestUser,
          )
        }

        assertThat(newPlacementApplication.application).isEqualTo(application)
        assertThat(newPlacementApplication.allocatedToUser).isEqualTo(assigneeUser)
        assertThat(newPlacementApplication.createdByUser).isEqualTo(previousPlacementApplication.createdByUser)
        assertThat(newPlacementApplication.data).isEqualTo(previousPlacementApplication.data)
        assertThat(newPlacementApplication.document).isEqualTo(previousPlacementApplication.document)
        assertThat(newPlacementApplication.placementType).isEqualTo(previousPlacementApplication.placementType)
        assertThat(newPlacementApplication.dueAt).isEqualTo(dueAt)
        assertThat(newPlacementApplication.requestedDuration).isEqualTo(12)
        assertThat(newPlacementApplication.expectedArrival).isEqualTo(LocalDate.now())

        verify(exactly = 1) {
          cas1PlacementApplicationEmailService.placementApplicationAllocated(newPlacementApplication)
        }
      }
    }

    @Test
    fun `Reallocating a placement application that doesnt exist returns not found`() {
      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns null

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isNotFound("placement application", previousPlacementApplication.id)
    }

    @Test
    fun `Reallocating a placement application with a decision returns a General Validation Error`() {
      previousPlacementApplication.apply {
        decision = PlacementApplicationDecision.ACCEPTED
      }

      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isGeneralValidationError("This placement application has already been completed")
    }

    @Test
    fun `Reallocating a placement application that is already reallocated returns a Conflict Validation Error`() {
      previousPlacementApplication.apply {
        reallocatedAt = OffsetDateTime.now()
      }

      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isConflictError()
        .hasEntityId(previousPlacementApplication.id)
        .hasMessage("This placement application has already been reallocated")
    }

    @Test
    fun `Reallocating a placement application when user to assign to is not a MATCHER or ASSESSOR returns a field validation error`() {
      every { lockablePlacementApplicationRepository.acquirePessimisticLock(previousPlacementApplication.id) } returns null
      every { placementApplicationRepository.findByIdOrNull(previousPlacementApplication.id) } returns previousPlacementApplication

      val result = cas1PlacementApplicationService.reallocateApplication(assigneeUser, previousPlacementApplication.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.userId", "lackingMatcherRole")
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

      val result = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState not withdrawable if already withdrawn`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withIsWithdrawn(true)
        .withReallocatedAt(null)
        .produce()

      every { userAccessService.userMayWithdrawPlacementApplication(user, placementApplication) } returns true

      val result = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawn).isTrue()
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

      val result = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)

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

      val result = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)

      assertThat(result.withdrawn).isFalse()
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

      val result = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)

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

      val submittedTimeStamp = OffsetDateTime.now().minusSeconds(1)
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(allocatedTo)
        .withDecision(null)
        .withCreatedByUser(user)
        .withDecisionMadeAt(submittedTimeStamp)
        .withSubmittedAt(OffsetDateTime.now())
        .withIsWithdrawn(false)
        .produce()

      val withdrawalContext = WithdrawalContext(
        WithdrawalTriggeredByUser(user),
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
      )

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(placementApplication, withdrawalContext, any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(any(), any(), any()) } returns Unit

      val result = cas1PlacementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = withdrawalContext,
      )

      assertThat(result is CasResult.Success).isTrue
      val entity = (result as CasResult.Success).value

      assertThat(entity.decision).isEqualTo(null)
      assertThat(entity.decisionMadeAt).isEqualTo(submittedTimeStamp)
      assertThat(entity.isWithdrawn).isEqualTo(true)

      val placementApplicationAfterWithdrawn = placementApplication
      placementApplicationAfterWithdrawn.isWithdrawn = true
      verify { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(placementApplicationAfterWithdrawn, allocatedTo, WithdrawalTriggeredByUser(user)) }
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
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(any(), any(), any()) } returns Unit
      every { cas1PlacementApplicationEmailService.placementApplicationWithdrawn(any(), any(), any()) } returns Unit

      val result = cas1PlacementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
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
      every { placementApplicationRepository.save(any()) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      assertThatThrownBy {
        cas1PlacementApplicationService.withdrawPlacementApplication(
          placementApplication.id,
          PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
          withdrawalContext = WithdrawalContext(
            WithdrawalTriggeredByUser(user),
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
        .withIsWithdrawn(true)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = cas1PlacementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
        ),
      )

      assertThatCasResult(result).isSuccess()
    }
  }
}
