package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequirementsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertAssessmentHasSystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AssessmentServiceTest {
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
  private val offenderServiceMock = mockk<OffenderService>()
  private val placementRequestServiceMock = mockk<Cas1PlacementRequestService>()
  private val cas1PlacementRequirementsServiceMock = mockk<Cas1PlacementRequirementsService>()
  private val objectMapperMock = spyk<ObjectMapper>()
  private val cas1TaskDeadlineServiceMock = mockk<Cas1TaskDeadlineService>()
  private val cas1AssessmentEmailServiceMock = mockk<Cas1AssessmentEmailService>()
  private val cas1AssessmentDomainEventService = mockk<Cas1AssessmentDomainEventService>()
  private val cas1PlacementRequestEmailService = mockk<Cas1PlacementRequestEmailService>()
  private val cas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val lockableAssessmentRepository = mockk<LockableAssessmentRepository>()
  private val cas1AssessmentService = mockk<Cas1AssessmentService>()

  private val assessmentService = AssessmentService(
    userServiceMock,
    userAccessServiceMock,
    assessmentRepositoryMock,
    assessmentReferralHistoryNoteRepositoryMock,
    offenderServiceMock,
    placementRequestServiceMock,
    cas1PlacementRequirementsServiceMock,
    objectMapperMock,
    cas1TaskDeadlineServiceMock,
    cas1AssessmentEmailServiceMock,
    cas1AssessmentDomainEventService,
    cas1PlacementRequestEmailService,
    cas1ApplicationStatusService,
    Clock.systemDefaultZone(),
    lockableAssessmentRepository,
    cas1AssessmentService,
  )

  @Test
  fun `getAssessmentAndValidate gets assessment when user is authorised to view assessment`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce(),
      )
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withYieldedProbationRegion {
                ProbationRegionEntityFactory()
                  .withYieldedApArea { ApAreaEntityFactory().produce() }
                  .produce()
              }
              .produce(),
          )
          .produce(),
      )
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
    every {
      offenderServiceMock.getOffenderByCrn(
        assessment.application.crn,
        user.deliusUsername,
      )
    } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    every { offenderServiceMock.getPersonSummaryInfoResult(assessment.application.crn, user.cas1LaoStrategy()) } returns
      PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce())

    val result = assessmentService.getAssessmentAndValidate(user, assessment.id)

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success
    assertThat(result.value).isEqualTo(assessment)
  }

  @Test
  fun `getAssessmentAndValidate does not get assessment when user is not authorised to view assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val assessment =
      ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(
              UserEntityFactory()
                .withYieldedProbationRegion {
                  ProbationRegionEntityFactory()
                    .withYieldedApArea { ApAreaEntityFactory().produce() }
                    .produce()
                }
                .produce(),
            )
            .produce(),
        )
        .produce()

    every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns false

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    val result = assessmentService.getAssessmentAndValidate(user, assessmentId)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `getAssessmentAndValidate returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null

    val result = assessmentService.getAssessmentAndValidate(user, assessmentId) as CasResult.NotFound

    assertThat(result.id).isEqualTo(assessmentId.toString())
    assertThat(result.entityType).isEqualTo("AssessmentEntity")
  }

  @Test
  fun `addReferralHistoryUserNote returns not found for non-existent Assessment`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null

    val result = assessmentService.addAssessmentReferralHistoryUserNote(user, assessmentId, "referral history note")

    assertThat(result is CasResult.NotFound).isTrue()
  }

  @Test
  fun `closeAssessment returns unauthorised when the user does not have permission to access the assessment`() {
    val assessmentId = UUID.randomUUID()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns TemporaryAccommodationAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

    val result = assessmentService.closeAssessment(user, assessmentId)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `closeAssessment returns OK for Assessment where it has already been closed`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.ACCEPTED)
      .withAllocatedToUser(user)
      .withCompletedAt(OffsetDateTime.now())
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { offenderServiceMock.getPersonSummaryInfoResult(assessment.application.crn, user.cas1LaoStrategy()) } returns
      PersonSummaryInfoResult.Success.Full(
        "crn1",
        CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
      )

    val result = assessmentService.closeAssessment(user, assessment.id)

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success
    assertThat(result.value.id).isEqualTo(assessment.id)
  }

  @Test
  fun `closeAssessment returns updated assessment`() {
    val assessmentId = UUID.randomUUID()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withId(assessmentId)
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withAllocatedToUser(user)
      .withData("{\"test\": \"data\"}")
      .produce()

    every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

    every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { offenderServiceMock.getPersonSummaryInfoResult(assessment.application.crn, user.cas1LaoStrategy()) } returns
      PersonSummaryInfoResult.Success.Full(
        "crn1",
        CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
      )

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.closeAssessment(user, assessmentId)

    assertThat(result is CasResult.Success).isTrue
    result as CasResult.Success
    val updatedAssessment = result.value
    assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(updatedAssessment is TemporaryAccommodationAssessmentEntity)
    updatedAssessment as TemporaryAccommodationAssessmentEntity
    assertThat(updatedAssessment.completedAt).isNotNull()
    assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.COMPLETED)
  }

  @Nested
  inner class ReallocateAssessment {
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

    private val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
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
      .produce()

    private val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val actingUser = UserEntityFactory()
      .withDeliusUsername("Acting User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }.produce()

    @Test
    fun `reallocateAssessment for Approved Premises returns General Validation Error when application already has a submitted assessment`() {
      previousAssessment.apply {
        submittedAt = OffsetDateTime.now()
      }

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isGeneralValidationError("A decision has already been taken on this assessment")
    }

    @Test
    fun `reallocateAssessment for Approved Premises returns Field Validation Error when user to assign to does not have cas1 assess application permission`() {
      assigneeUser.apply {
        roles = mutableListOf()
      }

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment
      every { cas1AssessmentService.getRequiredQualificationsToAssess(application) } returns emptyList()

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.userId", "lacking assess application or assess appealed application permission")
    }

    @Test
    fun `reallocateAssessment for appealed application returns Field Validation Error when user to assign to does not have cas1 assess appealed application permission`() {
      assigneeUser.apply {
        roles = mutableListOf()
      }

      previousAssessment.apply {
        this.isWithdrawn = true
      }

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment
      every { cas1AssessmentService.getRequiredQualificationsToAssess(application) } returns emptyList()

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.userId", "lacking assess application or assess appealed application permission")
    }

    @Test
    fun `reallocateAssessment for Approved Premises returns Field Validation Error when user to assign to does not have relevant qualifications`() {
      val assigneeUser = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(UserRole.CAS1_ASSESSOR)
            .produce()
        }

      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment
      every { cas1AssessmentService.getRequiredQualificationsToAssess(application) } returns listOf(UserQualification.PIPE)

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.userId", "lackingQualifications")
    }

    @Test
    fun `reallocateAssessment for Approved Premises returns Conflict Error when assessment has already been reallocated`() {
      previousAssessment.apply {
        reallocatedAt = OffsetDateTime.now()
      }

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isConflictError().hasMessage("This assessment has already been reallocated")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `reallocateAssessment for Approved Premises returns Success, deallocates old assessment and creates a new one, sends allocation & deallocation emails and domain events`(
      createdFromAppeal: Boolean,
    ) {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()

        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.PIPE)
          .produce()
      }

      previousAssessment.apply {
        this.createdFromAppeal = createdFromAppeal
      }

      val dueAt = OffsetDateTime.now()

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment
      every { cas1AssessmentService.getRequiredQualificationsToAssess(application) } returns emptyList()

      every { cas1ApplicationStatusService.assessmentCreated(any()) } returns Unit
      every { cas1ApplicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { cas1AssessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs

      every { cas1AssessmentEmailServiceMock.assessmentDeallocated(any(), any(), any()) } just Runs

      every { cas1TaskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isSuccess().with { newAssessment ->

        assertThat(previousAssessment.reallocatedAt).isNotNull
        assertThat((newAssessment as ApprovedPremisesAssessmentEntity).createdFromAppeal).isEqualTo(createdFromAppeal)
        assertThat(newAssessment.dueAt).isEqualTo(dueAt)

        verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }

        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentAllocated(
            match { it.id == assigneeUser.id },
            any<UUID>(),
            application,
            dueAt,
            false,
          )
        }

        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentDeallocated(
            match { it.id == previousAssessment.allocatedToUser!!.id },
            any<UUID>(),
            application,
          )
        }

        verify {
          cas1AssessmentDomainEventService.assessmentAllocated(any(), assigneeUser, actingUser)
        }
      }
    }

    @Test
    fun `reallocateAssessment for appealed application returns Success when user has cas1 assess appealed application permission`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()

        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.PIPE)
          .produce()
      }

      previousAssessment.apply {
        this.createdFromAppeal = true
        this.isWithdrawn = true
      }

      val dueAt = OffsetDateTime.now()

      every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
        previousAssessment.id,
      )
      every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment
      every { cas1AssessmentService.getRequiredQualificationsToAssess(application) } returns emptyList()

      every { cas1ApplicationStatusService.assessmentCreated(any()) } returns Unit
      every { cas1ApplicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { cas1AssessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs

      every { cas1AssessmentEmailServiceMock.assessmentDeallocated(any(), any(), any()) } just Runs

      every { cas1TaskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      val result = assessmentService.reallocateAssessment(
        allocatingUser = actingUser,
        assigneeUser = assigneeUser,
        id = previousAssessment.id,
      )

      assertThatCasResult(result).isSuccess().with { newAssessment ->

        assertThat(previousAssessment.reallocatedAt).isNotNull
        assertThat((newAssessment as ApprovedPremisesAssessmentEntity).createdFromAppeal).isEqualTo(true)
        assertThat(newAssessment.dueAt).isEqualTo(dueAt)

        verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }

        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentAllocated(
            match { it.id == assigneeUser.id },
            any<UUID>(),
            application,
            dueAt,
            false,
          )
        }

        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentDeallocated(
            match { it.id == previousAssessment.allocatedToUser!!.id },
            any<UUID>(),
            application,
          )
        }

        verify {
          cas1AssessmentDomainEventService.assessmentAllocated(any(), assigneeUser, actingUser)
        }
      }
    }
  }

  @Test
  fun `reallocateAssessment for Temporary Accommodation returns Field Validation Error when user to assign to is not an ASSESSOR`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val assigneeUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val actingUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withProbationRegion(probationRegion)
      .produce()

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
      (previousAssessment.id),
    )

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    val result = assessmentService.reallocateAssessment(
      allocatingUser = actingUser,
      assigneeUser = assigneeUser,
      id = previousAssessment.id,
    )

    assertThatCasResult(result).isFieldValidationError().hasMessage("$.userId", "lackingAssessorRole")
  }

  @Test
  fun `reallocateAssessment for Temporary Accommodation returns Success and updates the assigned user in place`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val assigneeUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS3_ASSESSOR)
          .produce()
      }

    val actingUser = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withProbationRegion(probationRegion)
      .produce()

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .produce()

    every { lockableAssessmentRepository.acquirePessimisticLock(previousAssessment.id) } returns LockableAssessmentEntity(
      previousAssessment.id,
    )
    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { userServiceMock.getUserForRequest() } returns assigneeUser
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.reallocateAssessment(
      allocatingUser = actingUser,
      assigneeUser = assigneeUser,
      id = previousAssessment.id,
    )

    assertThatCasResult(result).isSuccess().with {
      assertThat(it).isEqualTo(previousAssessment)
      assertAssessmentHasSystemNote(it, assigneeUser, ReferralHistorySystemNoteType.IN_REVIEW)

      verify { assessmentRepositoryMock.save(match { it.allocatedToUser == assigneeUser }) }
    }
  }

  @Test
  fun `deallocateAssessment throws an exception when the assessment is not a Temporary Accommodation assessment`() {
    val application = ApprovedPremisesApplicationEntityFactory()
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

    val previousAssessment = ApprovedPremisesAssessmentEntityFactory()
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
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    assertThatThrownBy { assessmentService.deallocateAssessment(previousAssessment.id) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Only CAS3 Assessments are currently supported")
  }

  @Test
  fun `deallocateAssessment returns a NotFound error if the assessment could not be found`() {
    every { assessmentRepositoryMock.findByIdOrNull(any()) } returns null

    val id = UUID.randomUUID()

    val result = assessmentService.deallocateAssessment(id)

    assertThatCasResult(result).isNotFound("assessment", id.toString())
  }

  @Test
  fun `deallocateAssessment returns Success, removes the assigned user in place, and unsets any decision made`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val previousAssessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withDecision(AssessmentDecision.REJECTED)
      .produce()

    every { assessmentRepositoryMock.findByIdOrNull(previousAssessment.id) } returns previousAssessment

    every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

    every { userServiceMock.getUserForRequest() } returns user
    every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

    val result = assessmentService.deallocateAssessment(previousAssessment.id)

    assertThatCasResult(result).isSuccess().with {
      assertThat(it).isEqualTo(previousAssessment)
      assertAssessmentHasSystemNote(it, user, ReferralHistorySystemNoteType.UNALLOCATED)
    }

    verify {
      assessmentRepositoryMock.save(
        match {
          it.allocatedToUser == null &&
            it.decision == null &&
            it.submittedAt == null
        },
      )
    }
  }

  @Nested
  inner class CreateAssessments {

    private val actingUser = UserEntityFactory()
      .withDeliusUsername("Acting User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }.produce()

    @Test
    fun `create CAS3 Assessment`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withProbationRegion(probationRegion)
            .produce(),
        )
        .withProbationRegion(probationRegion)
        .produce()

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

      every { userServiceMock.getUserForRequest() } returns actingUser
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val summaryData = object {
        val num = 50
        val text = "Hello world!"
      }

      val result = assessmentService.createTemporaryAccommodationAssessment(application, summaryData)

      assertAssessmentHasSystemNote(result, actingUser, ReferralHistorySystemNoteType.SUBMITTED)
      assertThat(result.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")

      verify { assessmentRepositoryMock.save(match { it.application == application }) }
    }
  }

  @Nested
  inner class AcceptAssessment {

    lateinit var user: UserEntity
    lateinit var assessmentId: UUID

    private lateinit var assessmentFactory: ApprovedPremisesAssessmentEntityFactory
    private lateinit var placementRequirements: PlacementRequirements

    @BeforeEach
    fun setup() {
      user = UserEntityFactory().withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.produce()

      assessmentId = UUID.randomUUID()

      assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(
              UserEntityFactory()
                .withYieldedProbationRegion {
                  ProbationRegionEntityFactory()
                    .withYieldedApArea { ApAreaEntityFactory().produce() }
                    .produce()
                }
                .produce(),
            )
            .produce(),
        )
        .withAllocatedToUser(user)
        .withData("{\"test\": \"data\"}")

      placementRequirements = PlacementRequirements(
        type = ApType.normal,
        location = "AB123",
        radius = 50,
        desirableCriteria = listOf(),
        essentialCriteria = listOf(),
      )
    }

    @Test
    fun `CAS1 unauthorised when no user is allocated to the assessment`() {
      val assessment = assessmentFactory
        .withAllocatedToUser(null)
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { userServiceMock.getUserForRequest() } returns user

      val result = assessmentService.acceptAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        placementRequirements,
        null,
        null,
        null,
      )

      assertThatCasResult(result).isGeneralValidationError("An assessment must be allocated to a user to be updated")
    }

    @Test
    fun `CAS1 unauthorised when submitted user is not allocated to the assessment`() {
      val assessment = assessmentFactory
        .withAllocatedToUser(UserEntityFactory().withDefaults().produce())
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { userServiceMock.getUserForRequest() } returns user

      val result = assessmentService.acceptAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        placementRequirements,
        null,
        null,
        null,
      )

      assertThatCasResult(result).isUnauthorised("The assessment can only be updated by the allocated user")
    }

    @Test
    fun `unauthorised when the user does not have permissions to access the assessment`() {
      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessmentFactory
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

      val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isUnauthorised("Not authorised to view the assessment")
    }

    @Test
    fun `general validation error where decision has already been taken`() {
      val assessment = assessmentFactory
        .withDecision(AssessmentDecision.ACCEPTED)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isGeneralValidationError("A decision has already been taken on this assessment")
    }

    @Test
    fun `general validation error where assessment has been deallocated`() {
      val assessment = assessmentFactory
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = assessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isGeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    @Test
    fun `unauthorised when user not allowed to view Offender (LAO)`() {
      val assessment = assessmentFactory.produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

      every {
        offenderServiceMock.getOffenderByCrn(
          assessment.application.crn,
          user.deliusUsername,
        )
      } returns AuthorisableActionResult.Unauthorised()

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns PersonSummaryInfoResult.NotFound(
        assessment.application.crn,
      )

      val result = assessmentService.acceptAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        placementRequirements,
        null,
        null,
        null,
      )

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `CAS1 success returns updated assessment, emits domain event, sends email, does not create placement request when no date information provided`() {
      val assessment = assessmentFactory.produce()
      val application = assessment.application as ApprovedPremisesApplicationEntity

      val placementRequirementEntity = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { cas1ApplicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every { userServiceMock.getUserForRequest() } returns user

      every {
        cas1PlacementRequirementsServiceMock.createPlacementRequirements(
          assessment,
          placementRequirements,
        )
      } returns placementRequirementEntity

      every { cas1AssessmentDomainEventService.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

      every { cas1AssessmentEmailServiceMock.assessmentAccepted(any()) } just Runs

      val result = assessmentService.acceptAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        placementRequirements,
        null,
        null,
        null,
      )

      assertThatCasResult(result).isSuccess().with { updatedAssessment ->

        assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
        assertThat(updatedAssessment.submittedAt).isNotNull()
        assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

        verify(exactly = 0) {
          placementRequestServiceMock.createPlacementRequest(any(), any(), any(), false, null)
        }

        verify(exactly = 1) {
          cas1AssessmentDomainEventService.assessmentAccepted(application, any(), any(), any(), any(), any())
        }
        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentAccepted(application)
        }
      }
    }

    @Test
    fun `CAS1 returns updated assessment, emits domain event, sends emails, creates placement request when requirements provided`() {
      val assessment = assessmentFactory.produce()
      val application = assessment.application as ApprovedPremisesApplicationEntity

      val placementRequirementEntity = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )

      val notes = "Some Notes"

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { cas1ApplicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as AssessmentEntity }

      every {
        cas1PlacementRequirementsServiceMock.createPlacementRequirements(
          assessment,
          placementRequirements,
        )
      } returns placementRequirementEntity

      every {
        placementRequestServiceMock.createPlacementRequest(
          any(),
          any(),
          any(),
          any(),
          any(),
        )
      } returns PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
        .withAssessment(assessment)
        .produce()

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { userServiceMock.getUserForRequest() } returns user

      every { cas1AssessmentEmailServiceMock.assessmentAccepted(any()) } just Runs

      every { cas1AssessmentDomainEventService.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

      every { cas1PlacementRequestEmailService.placementRequestSubmitted(any()) } just Runs

      val result = assessmentService.acceptAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        placementRequirements,
        placementDates,
        null,
        notes,
      )

      assertThatCasResult(result).isSuccess().with { updatedAssessment ->
        assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
        assertThat(updatedAssessment.submittedAt).isNotNull()
        assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

        verify(exactly = 1) {
          placementRequestServiceMock.createPlacementRequest(
            placementRequirements = placementRequirementEntity,
            placementDates = placementDates,
            notes = notes,
            isParole = false,
            placementApplicationEntity = null,
          )
        }

        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentAccepted(application)
        }

        verify(exactly = 1) {
          cas1AssessmentDomainEventService.assessmentAccepted(application, any(), any(), any(), any(), any())
        }

        verify(exactly = 1) {
          cas1PlacementRequestEmailService.placementRequestSubmitted(assessment.application as ApprovedPremisesApplicationEntity)
        }
      }
    }

    @Test
    fun `CAS3 sets completed at timestamp to null`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(probationRegion)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withCompletedAt(OffsetDateTime.now())
        .withDecision(AssessmentDecision.REJECTED)
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername, true) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { cas1AssessmentDomainEventService.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

      every { userServiceMock.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val result =
        assessmentService.acceptAssessment(user, assessmentId, "{\"test\": \"data\"}", null, null, null, null)

      assertThatCasResult(result).isSuccess().with { updatedAssessment ->
        updatedAssessment as TemporaryAccommodationAssessmentEntity
        assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
        assertThat(updatedAssessment.submittedAt).isNotNull()
        assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
        assertThat(updatedAssessment.completedAt).isNull()
        assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.READY_TO_PLACE)
      }
    }
  }
}
