package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationPlaceholderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequirementsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentServiceTest {
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val assessmentClarificationNoteRepositoryMock = mockk<AssessmentClarificationNoteRepository>()
  private val offenderServiceMock = mockk<OffenderService>()
  private val placementRequestServiceMock = mockk<Cas1PlacementRequestService>()
  private val cas1PlacementRequirementsServiceMock = mockk<Cas1PlacementRequirementsService>()
  private val cas1AssessmentEmailServiceMock = mockk<Cas1AssessmentEmailService>()
  private val cas1AssessmentDomainEventService = mockk<Cas1AssessmentDomainEventService>()
  private val cas1PlacementRequestEmailService = mockk<Cas1PlacementRequestEmailService>()
  private val applicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val assessmentClarificationNoteListener = mockk<AssessmentClarificationNoteListener>()
  private val approvedPremisesAssessmentRepositoryMock = mockk<ApprovedPremisesAssessmentRepository>()
  private val lockableAssessmentRepositoryMock = mockk<LockableAssessmentRepository>()
  private val userAllocatorMock = mockk<UserAllocator>()
  private val cas1TaskDeadlineServiceMock = mockk<Cas1TaskDeadlineService>()
  private val cas1PlacementApplicationServiceMock = mockk<Cas1PlacementApplicationService>()
  private val placementApplicationPlaceholderRepositoryMock = mockk<PlacementApplicationPlaceholderRepository>()

  private val cas1AssessmentService = Cas1AssessmentService(
    userAccessServiceMock,
    assessmentRepositoryMock,
    assessmentClarificationNoteRepositoryMock,
    offenderServiceMock,
    placementRequestServiceMock,
    cas1PlacementRequirementsServiceMock,
    cas1AssessmentEmailServiceMock,
    cas1AssessmentDomainEventService,
    cas1PlacementRequestEmailService,
    applicationStatusService,
    assessmentClarificationNoteListener,
    approvedPremisesAssessmentRepositoryMock,
    lockableAssessmentRepositoryMock,
    cas1TaskDeadlineServiceMock,
    userAllocatorMock,
    placementApplicationPlaceholderRepositoryMock,
    cas1PlacementApplicationServiceMock,
    Clock.systemDefaultZone(),
  )

  @Test
  fun `findApprovedPremisesAssessmentSummariesNotReallocatedForUser only fetches Approved Premises assessments allocated to the user that have not been reallocated`() {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    user.roles.add(
      UserRoleAssignmentEntityFactory()
        .withRole(UserRole.CAS1_ASSESSOR)
        .withUser(user)
        .produce(),
    )

    every {
      assessmentRepositoryMock.findAllApprovedPremisesAssessmentSummariesNotReallocated(
        any(),
        listOf("NOT_STARTED", "IN_PROGRESS"),
        PageRequest.of(4, 7, Sort.by("status").ascending()),
      )
    } returns Page.empty()

    cas1AssessmentService.findApprovedPremisesAssessmentSummariesNotReallocatedForUser(
      user,
      statuses = listOf(DomainAssessmentSummaryStatus.NOT_STARTED, DomainAssessmentSummaryStatus.IN_PROGRESS),
      PageCriteria(
        sortBy = Cas1AssessmentSortField.assessmentStatus,
        sortDirection = SortDirection.asc,
        page = 5,
        perPage = 7,
      ),
    )

    verify(exactly = 1) {
      assessmentRepositoryMock.findAllApprovedPremisesAssessmentSummariesNotReallocated(
        user.id.toString(),
        listOf("NOT_STARTED", "IN_PROGRESS"),
        PageRequest.of(4, 7, Sort.by("status").ascending()),
      )
    }
  }

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

    every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

    every { offenderServiceMock.getPersonSummaryInfoResult(assessment.application.crn, user.cas1LaoStrategy()) } returns
      PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce())

    val result = cas1AssessmentService.getAssessmentAndValidate(user, assessment.id)

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

    every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

    val result = cas1AssessmentService.getAssessmentAndValidate(user, assessmentId)

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

    every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null

    val result = cas1AssessmentService.getAssessmentAndValidate(user, assessmentId) as CasResult.NotFound

    assertThat(result.id).isEqualTo(assessmentId.toString())
    assertThat(result.entityType).isEqualTo("AssessmentEntity")
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

    @ParameterizedTest
    @CsvSource(
      value = [
        "emergency,true", "standard,true", "shortNotice,true",
        "emergency,false", "standard,false", "shortNotice,false",
      ],
    )
    fun `create CAS1 Assessment creates an Assessment, sends allocation email and allocated domain event`(
      timelinessCategory: Cas1ApplicationTimelinessCategory,
      createdFromAppeal: Boolean,
    ) {
      val userWithLeastAllocatedAssessments = UserEntityFactory()
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

          qualifications += UserQualificationAssignmentEntityFactory()
            .withUser(this)
            .withQualification(UserQualification.PIPE)
            .produce()

          if (timelinessCategory != Cas1ApplicationTimelinessCategory.standard) {
            qualifications += UserQualificationAssignmentEntityFactory()
              .withUser(this)
              .withQualification(UserQualification.EMERGENCY)
              .produce()
          }
        }

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
        .withApType(ApprovedPremisesType.PIPE)
        .withNoticeType(timelinessCategory)
        .produce()

      val dueAt = OffsetDateTime.now()

      every { applicationStatusService.assessmentCreated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { userAllocatorMock.getUserForAssessmentAllocation(any()) } returns userWithLeastAllocatedAssessments

      every { cas1TaskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      if (createdFromAppeal) {
        every { cas1AssessmentEmailServiceMock.appealedAssessmentAllocated(any(), any(), any()) } just Runs
      } else {
        every { cas1AssessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs
      }

      every { userServiceMock.getUserForRequest() } returns actingUser

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      val assessment = cas1AssessmentService.createAssessment(application, createdFromAppeal)

      verify { assessmentRepositoryMock.save(match { it.allocatedToUser == userWithLeastAllocatedAssessments && it.dueAt == dueAt }) }

      if (createdFromAppeal) {
        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.appealedAssessmentAllocated(
            match { it.id == userWithLeastAllocatedAssessments.id },
            any<UUID>(),
            application,
          )
        }
      } else {
        verify(exactly = 1) {
          cas1AssessmentEmailServiceMock.assessmentAllocated(
            match { it.id == userWithLeastAllocatedAssessments.id },
            any<UUID>(),
            application,
            dueAt,
            timelinessCategory == Cas1ApplicationTimelinessCategory.emergency,
          )
        }

        verify { applicationStatusService.assessmentCreated(assessment) }
      }

      verify {
        cas1AssessmentDomainEventService.assessmentAllocated(
          assessment,
          allocatedToUser = userWithLeastAllocatedAssessments,
          allocatingUser = null,
        )
      }
    }

    @Test
    fun `create CAS1 Assessment doesn't create allocated domain event if no suitable allocation found`() {
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
        .withApType(ApprovedPremisesType.PIPE)
        .withNoticeType(Cas1ApplicationTimelinessCategory.shortNotice)
        .produce()

      val dueAt = OffsetDateTime.now()

      every { applicationStatusService.assessmentCreated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { userAllocatorMock.getUserForAssessmentAllocation(any()) } returns null

      every { cas1TaskDeadlineServiceMock.getDeadline(any<ApprovedPremisesAssessmentEntity>()) } returns dueAt

      every { cas1AssessmentEmailServiceMock.assessmentAllocated(any(), any(), any(), any(), any()) } just Runs

      every { userServiceMock.getUserForRequest() } returns actingUser

      every { cas1AssessmentDomainEventService.assessmentAllocated(any(), any(), any()) } just Runs

      cas1AssessmentService.createAssessment(application, createdFromAppeal = false)

      verify { cas1AssessmentDomainEventService wasNot Called }
    }
  }

  @Nested
  inner class AddAssessmentClarificationNote {
    @Test
    fun `addAssessmentClarificationNote returns not found for non-existent Assessment`() {
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null

      val result = cas1AssessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `addAssessmentClarificationNote returns unauthorised when the user does not have permission to access the assessment`() {
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
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

      val result = cas1AssessmentService.addAssessmentClarificationNote(user, assessmentId, "clarification note")

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `addAssessmentClarificationNote adds note to assessment allocated to different user`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
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

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { assessmentClarificationNoteListener.prePersist(any()) } returns Unit
      every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
        it.invocation.args[0] as AssessmentClarificationNoteEntity
      }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { cas1AssessmentDomainEventService.furtherInformationRequested(any(), any()) } just Runs

      val text = "clarification note"
      val result = cas1AssessmentService.addAssessmentClarificationNote(user, assessment.id, text)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      every { assessmentClarificationNoteListener.prePersist(any()) } returns Unit
      verify(exactly = 1) {
        assessmentClarificationNoteRepositoryMock.save(
          match {
            it.assessment == assessment &&
              it.createdByUser == user &&
              it.query == text
          },
        )
      }

      verify(exactly = 1) {
        cas1AssessmentDomainEventService.furtherInformationRequested(assessment, result.value)
      }
    }

    @Test
    fun `addAssessmentClarificationNote adds note to assessment allocated to calling user`() {
      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
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
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { assessmentClarificationNoteListener.prePersist(any()) } returns Unit
      every { assessmentClarificationNoteRepositoryMock.save(any()) } answers {
        it.invocation.args[0] as AssessmentClarificationNoteEntity
      }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      every { cas1AssessmentDomainEventService.furtherInformationRequested(any(), any()) } just Runs

      val text = "clarification note"
      val result = cas1AssessmentService.addAssessmentClarificationNote(user, assessment.id, text)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      every { assessmentClarificationNoteListener.prePersist(any()) } returns Unit
      verify(exactly = 1) {
        assessmentClarificationNoteRepositoryMock.save(
          match {
            it.assessment == assessment &&
              it.createdByUser == user &&
              it.query == text
          },
        )
      }

      verify(exactly = 1) {
        cas1AssessmentDomainEventService.furtherInformationRequested(assessment, result.value)
      }
    }
  }

  @Nested
  inner class UpdateAssessment {
    val user = UserEntityFactory()
      .withDefaults()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    @Test
    fun `CAS1 error if not allocated to a user`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(null)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

      assertThatCasResult(result).isGeneralValidationError("An assessment must be allocated to a user to be updated")
    }

    @Test
    fun `CAS1 unauthorised when not allocated to the user`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaults().produce())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

      assertThatCasResult(result).isUnauthorised("The assessment can only be updated by the allocated user")
    }

    @Test
    fun `unauthorised when the user does not have permission to access the assessment`() {
      val assessmentId = UUID.randomUUID()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

      val result = cas1AssessmentService.updateAssessment(user, assessmentId, "{}")

      assertThatCasResult(result).isUnauthorised("Not authorised to view the assessment")
    }

    @Test
    fun `general validation error where assessment is withdrawn`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(AssessmentDecision.ACCEPTED)
        .withAllocatedToUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{}")

      assertThatCasResult(result).isGeneralValidationError("The application has been withdrawn.")
    }

    @Test
    fun `general validation error where decision has already been taken`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(AssessmentDecision.ACCEPTED)
        .withAllocatedToUser(user)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{}")

      assertThatCasResult(result).isGeneralValidationError("A decision has already been taken on this assessment")
    }

    @Test
    fun `general validation error where assessment has been deallocated`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(null)
        .withDecision(null)
        .withAllocatedToUser(user)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{}")

      assertThatCasResult(result).isGeneralValidationError("The assessment has been reallocated, this assessment is read only")
    }

    @Test
    fun `unauthorised when user cannot view Offender (LAO)`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns PersonSummaryInfoResult.NotFound(assessment.application.crn)

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun success() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = cas1AssessmentService.updateAssessment(user, assessment.id, "{\"test\": \"data\"}")

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.data).isEqualTo("{\"test\": \"data\"}")
      }
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

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      every { userServiceMock.getUserForRequest() } returns user

      val result = cas1AssessmentService.acceptAssessment(
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

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      every { userServiceMock.getUserForRequest() } returns user

      val result = cas1AssessmentService.acceptAssessment(
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
      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessmentFactory
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

      val result =
        cas1AssessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isUnauthorised("Not authorised to view the assessment")
    }

    @Test
    fun `general validation error where decision has already been taken`() {
      val assessment = assessmentFactory
        .withDecision(AssessmentDecision.ACCEPTED)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result =
        cas1AssessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isGeneralValidationError("A decision has already been taken on this assessment")
    }

    @Test
    fun `general validation error where assessment has been deallocated`() {
      val assessment = assessmentFactory
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result =
        cas1AssessmentService.acceptAssessment(user, assessmentId, "{}", placementRequirements, null, null, null)

      assertThatCasResult(result).isGeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    @Test
    fun `unauthorised when user not allowed to view Offender (LAO)`() {
      val assessment = assessmentFactory.produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns PersonSummaryInfoResult.NotFound(assessment.application.crn)

      val result = cas1AssessmentService.acceptAssessment(
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

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          LaoStrategy.NeverRestricted,
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
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

      val result = cas1AssessmentService.acceptAssessment(
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
    fun `CAS1 returns updated assessment, emits domain event, sends emails, creates request for placement elements when requirements provided`() {
      val assessment = assessmentFactory.produce()
      val application = assessment.application as ApprovedPremisesApplicationEntity

      val placementRequirementEntity = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementDates = PlacementDates(
        expectedArrival = LocalDate.parse("2026-05-09"),
        duration = 12,
      )

      val notes = "Some Notes"

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        cas1PlacementRequirementsServiceMock.createPlacementRequirements(
          assessment,
          placementRequirements,
        )
      } returns placementRequirementEntity

      val placeholder = PlacementApplicationPlaceholderEntityFactory().produce()
      every {
        placementApplicationPlaceholderRepositoryMock.findByApplication(application)
      } returns placeholder

      val placementApplicationAutomatic = PlacementApplicationEntityFactory().withDefaults().produce()
      every {
        cas1PlacementApplicationServiceMock.createAutomaticPlacementApplication(
          id = placeholder.id,
          assessment = assessment,
          authorisedExpectedArrival = LocalDate.parse("2026-05-09"),
          authorisedDurationDays = 12,
        )
      } returns placementApplicationAutomatic

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

      val updatedPlaceholder = slot<PlacementApplicationPlaceholderEntity>()
      every { placementApplicationPlaceholderRepositoryMock.save(capture(updatedPlaceholder)) } returnsArgument 0

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          LaoStrategy.NeverRestricted,
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      every { userServiceMock.getUserForRequest() } returns user

      every { cas1AssessmentEmailServiceMock.assessmentAccepted(any()) } just Runs

      every { cas1AssessmentDomainEventService.assessmentAccepted(any(), any(), any(), any(), any(), any()) } just Runs

      every { cas1PlacementRequestEmailService.placementRequestSubmitted(any()) } just Runs

      val result = cas1AssessmentService.acceptAssessment(
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
            placementApplicationEntity = placementApplicationAutomatic,
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

        assertThat(updatedPlaceholder.captured.id).isEqualTo(placeholder.id)
        assertThat(updatedPlaceholder.captured.archived).isTrue
      }
    }
  }

  @Nested
  inner class RejectAssessment {
    val user = UserEntityFactory()
      .withDefaults()
      .withYieldedApArea {
        ApAreaEntityFactory()
          .withName("South West & South Central")
          .produce()
      }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(UserEntityFactory().withDefaults().produce())
      .produce()

    @Test
    fun `unauthorised when the user does not have permission to access the assessment`() {
      val assessmentId = UUID.randomUUID()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
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

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

      val result = cas1AssessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `general validation error when assessment doesnt have an allocated user`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(null)
        .withAllocatedToUser(null)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result = cas1AssessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

      assertThatCasResult(result).isGeneralValidationError("An assessment must be allocated to a user to be updated")
    }

    @Test
    fun `unauthorised when assessment doesnt have an allocated user`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(null)
        .withAllocatedToUser(UserEntityFactory().withDefaults().produce())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result = cas1AssessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

      assertThatCasResult(result).isUnauthorised("The assessment can only be updated by the allocated user")
    }

    @Test
    fun `general validation error for Assessment where decision has already been taken`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withDecision(AssessmentDecision.ACCEPTED)
        .withAllocatedToUser(user)
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result = cas1AssessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("A decision has already been taken on this assessment")
    }

    @Test
    fun `general validation error for Assessment where assessment has been deallocated`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(null)
        .withDecision(null)
        .withAllocatedToUser(user)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce(),
        )

      val result = cas1AssessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("The application has been reallocated, this assessment is read only")
    }

    @Test
    fun `unauthorised when user not allowed to view Offender (LAO)`() {
      val assessmentId = UUID.randomUUID()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withAllocatedToUser(user)
        .withData("{\"test\": \"data\"}")
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns PersonSummaryInfoResult.NotFound(assessment.application.crn)
      val result = cas1AssessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `success, returns updated assessment, triggers domain event, sends email`() {
      val assessmentId = UUID.randomUUID()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withAllocatedToUser(user)
        .withData("{\"test\": \"data\"}")
        .produce()

      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessmentId) } returns LockableAssessmentEntity(assessmentId)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { approvedPremisesAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { approvedPremisesAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }

      every { cas1AssessmentDomainEventService.assessmentRejected(any(), any(), any(), any()) } just Runs
      every { cas1AssessmentEmailServiceMock.assessmentRejected(any()) } just Runs

      val caseSummary =
        CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce()

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          caseSummary,
        )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          LaoStrategy.NeverRestricted,
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          "crn1",
          caseSummary,
        )

      val result = cas1AssessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success
      val updatedAssessment = result.value
      assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
      assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
      assertThat(updatedAssessment.submittedAt).isNotNull()
      assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")

      verify(exactly = 1) {
        cas1AssessmentDomainEventService.assessmentRejected(
          application,
          assessment,
          caseSummary.asOffenderDetailSummary(),
          user,
        )
      }

      verify(exactly = 1) {
        cas1AssessmentEmailServiceMock.assessmentRejected(application)
      }
    }
  }

  @Nested
  inner class UpdateAssessmentWithdrawn {
    private val assessmentId: UUID = UUID.randomUUID()

    private val allocatedUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .withEmail("user@test.com")
      .produce()

    private val withdrawingUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .withEmail("withdrawing@test.com")
      .produce()

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment pending if assessment active and allocated`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(null)
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { cas1AssessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any(), any()) } just Runs

      cas1AssessmentService.updateAssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        cas1AssessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          application,
          isAssessmentPending = true,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment withdrawn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(null)
        .withIsWithdrawn(true)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { cas1AssessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any(), any()) } just Runs

      cas1AssessmentService.updateAssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        cas1AssessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          application,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment submitted`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withReallocatedAt(null)
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { cas1AssessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any(), any()) } just Runs

      cas1AssessmentService.updateAssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        cas1AssessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          application,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }

    @Test
    fun `updateCas1AssessmentWithdrawn triggers email with assessment not pending if assessment reallocated`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withData("{\"test\": \"data\"}")
        .withAllocatedToUser(allocatedUser)
        .withSubmittedAt(null)
        .withReallocatedAt(OffsetDateTime.now())
        .withIsWithdrawn(false)
        .produce()

      every { assessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
      every { applicationStatusService.assessmentUpdated(any()) } returns Unit
      every { assessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesAssessmentEntity }
      every { cas1AssessmentEmailServiceMock.assessmentWithdrawn(any(), any(), any(), any()) } just Runs

      cas1AssessmentService.updateAssessmentWithdrawn(assessmentId, withdrawingUser)

      verify {
        cas1AssessmentEmailServiceMock.assessmentWithdrawn(
          assessment,
          application,
          isAssessmentPending = false,
          withdrawingUser = withdrawingUser,
        )
      }
    }
  }

  @Nested
  inner class GetRequiredQualificationsToAssess {

    @ParameterizedTest
    @CsvSource(
      "emergency,EMERGENCY",
      "shortNotice,EMERGENCY",
      "standard,",
    )
    fun `returns correctly for timeliness categories`(noticeType: Cas1ApplicationTimelinessCategory, qualification: UserQualification?) {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withNoticeType(noticeType)
        .produce()

      assertThat(cas1AssessmentService.getRequiredQualificationsToAssess(application)).isEqualTo(listOfNotNull(qualification))
    }

    @ParameterizedTest
    @CsvSource(
      "PIPE,PIPE",
      "ESAP,ESAP",
      "RFAP,RECOVERY_FOCUSED",
      "MHAP_ST_JOSEPHS,MENTAL_HEALTH_SPECIALIST",
      "MHAP_ELLIOTT_HOUSE,MENTAL_HEALTH_SPECIALIST",
    )
    fun `returns matching qualification for an application made to that type of premises`(apType: ApprovedPremisesType, qualification: UserQualification?) {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApType(apType)
        .produce()

      assertThat(cas1AssessmentService.getRequiredQualificationsToAssess(application)).isEqualTo(listOfNotNull(qualification))
    }
  }
}
