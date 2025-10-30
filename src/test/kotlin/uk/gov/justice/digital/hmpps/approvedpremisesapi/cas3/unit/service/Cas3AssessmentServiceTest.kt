package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ReferralRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertAssessmentHasSystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3AssessmentServiceTest {
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val temporaryAccommodationAssessmentRepositoryMock = mockk<TemporaryAccommodationAssessmentRepository>()
  private val lockableAssessmentRepositoryMock = mockk<LockableAssessmentRepository>()
  private val assessmentReferralHistoryNoteRepositoryMock = mockk<AssessmentReferralHistoryNoteRepository>()
  private val referralRejectionReasonRepositoryMock = mockk<ReferralRejectionReasonRepository>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val userServiceMock = mockk<UserService>()
  private val cas3DomainEventServiceMock = mockk<Cas3DomainEventService>()
  private val cas3DomainEventBuilderMock = mockk<Cas3DomainEventBuilder>()
  private val offenderServiceMock = mockk<OffenderService>()

  private val assessmentService = Cas3AssessmentService(
    assessmentRepositoryMock,
    temporaryAccommodationAssessmentRepositoryMock,
    referralRejectionReasonRepositoryMock,
    assessmentReferralHistoryNoteRepositoryMock,
    lockableAssessmentRepositoryMock,
    userAccessServiceMock,
    cas3DomainEventServiceMock,
    cas3DomainEventBuilderMock,
    userServiceMock,
    offenderServiceMock,
    Clock.systemDefaultZone(),
  )

  @Nested
  inner class GetAssessmentAndValidate {
    @Test
    fun `getAssessmentAndValidate gets assessment when user is authorised to view assessment`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val user = UserEntityFactory()
        .withYieldedProbationRegion { probationRegion }
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withAllocatedToUser(
          UserEntityFactory()
            .withYieldedProbationRegion { probationRegion }
            .produce(),
        )
        .withApplication(
          TemporaryAccommodationApplicationEntityFactory()
            .withProbationRegion(probationRegion)
            .withCreatedByUser(
              UserEntityFactory()
                .withYieldedProbationRegion { probationRegion }
                .produce(),
            )
            .produce(),
        )
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns true

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment
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

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val user = UserEntityFactory()
        .withYieldedProbationRegion { probationRegion }
        .produce()

      val assessment =
        TemporaryAccommodationAssessmentEntityFactory()
          .withId(assessmentId)
          .withAllocatedToUser(
            UserEntityFactory()
              .withYieldedProbationRegion { probationRegion }
              .produce(),
          )
          .withApplication(
            TemporaryAccommodationApplicationEntityFactory()
              .withProbationRegion(probationRegion)
              .withCreatedByUser(
                UserEntityFactory()
                  .withYieldedProbationRegion { probationRegion }
                  .produce(),
              )
              .produce(),
          )
          .produce()

      every { userAccessServiceMock.userCanViewAssessment(user, assessment) } returns false

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

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

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns null

      val result = assessmentService.getAssessmentAndValidate(user, assessmentId) as CasResult.NotFound

      assertThat(result.id).isEqualTo(assessmentId.toString())
      assertThat(result.entityType).isEqualTo("AssessmentEntity")
    }
  }

  @Nested
  inner class GetAssessmentSummariesForUser {
    @Test
    fun `getAssessmentSummariesForUser only fetches Temporary Accommodation assessments within the user's probation region`() {
      val pageCriteria = PageCriteria(
        sortBy = AssessmentSortField.assessmentStatus,
        sortDirection = SortDirection.asc,
        page = 5,
        perPage = 7,
      )
      val pageRequest = PageRequest.of(4, 7, Sort.by("status").ascending())

      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      user.roles.add(
        UserRoleAssignmentEntityFactory()
          .withRole(UserRole.CAS3_ASSESSOR)
          .withUser(user)
          .produce(),
      )

      every {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          any(),
          null,
          emptyList(),
          pageRequest,
        )
      } returns Page.empty()

      assessmentService.getAssessmentSummariesForUser(user, null, emptyList(), pageCriteria)

      verify(exactly = 1) {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          null,
          emptyList(),
          pageRequest,
        )
      }
    }

    @Test
    fun `getAssessmentSummariesForUser only fetches Temporary Accommodation assessments for the given CRN and within the user's probation region`() {
      val pageCriteria = PageCriteria(
        sortBy = AssessmentSortField.assessmentStatus,
        sortDirection = SortDirection.asc,
        page = 5,
        perPage = 7,
      )
      val pageRequest = PageRequest.of(4, 7, Sort.by("status").ascending())

      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      user.roles.add(
        UserRoleAssignmentEntityFactory()
          .withRole(UserRole.CAS3_ASSESSOR)
          .withUser(user)
          .produce(),
      )

      every {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          "SOMECRN",
          emptyList(),
          pageRequest,
        )
      } returns Page.empty()

      assessmentService.getAssessmentSummariesForUser(
        user,
        "SOMECRN",
        emptyList(),
        pageCriteria,
      )

      verify(exactly = 1) {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          "SOMECRN",
          emptyList(),
          pageRequest,
        )
      }
    }

    @Test
    fun `getAssessmentSummariesForUser only fetches Temporary Accommodation assessments sorted by default arrivalDate when requested sort field is personName`() {
      val pageCriteria = PageCriteria(
        sortBy = AssessmentSortField.personName,
        sortDirection = SortDirection.asc,
        page = 5,
        perPage = 7,
      )
      val pageRequest = PageRequest.of(4, 7, Sort.by("arrivalDate").ascending())

      val user = UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      user.roles.add(
        UserRoleAssignmentEntityFactory()
          .withRole(UserRole.CAS3_ASSESSOR)
          .withUser(user)
          .produce(),
      )

      every {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          "SOMECRN",
          listOf(),
          pageRequest,
        )
      } returns Page.empty()

      assessmentService.getAssessmentSummariesForUser(
        user,
        "SOMECRN",
        emptyList(),
        pageCriteria,
      )

      verify(exactly = 1) {
        assessmentRepositoryMock.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          "SOMECRN",
          emptyList(),
          pageRequest,
        )
      }
    }
  }

  @Nested
  inner class Cas3UpdateAssessment {
    @Test
    fun `an invalid assessment id returns a validation error`() {
      val assessmentId = UUID.randomUUID()
      val updateAssessment = updateAssessmentEntity(releaseDate = LocalDate.now(), accommodationRequiredFromDate = null)
      val user = UserEntityFactory().withDefaultProbationRegion().produce()

      every { assessmentRepositoryMock.findById(assessmentId) } returns (Optional.empty())

      val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment.releaseDate, updateAssessment.accommodationRequiredFromDate) as CasResult.NotFound
      assertAll(
        {
          assertThat(result.id).isEqualTo(assessmentId.toString())
          assertThat(result.entityType).isEqualTo(TemporaryAccommodationAssessmentEntity::class.simpleName)
        },
      )
    }

    @Test
    fun `user unable to access assessment returns unauthorised validation error`() {
      val assessmentId = UUID.randomUUID()

      val updateAssessment =
        updateAssessmentEntity(
          releaseDate = LocalDate.now(),
          accommodationRequiredFromDate = null,
        )

      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = assessmentEntity(user)

      every { assessmentRepositoryMock.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns false

      val result =
        assessmentService.updateAssessment(
          user,
          assessmentId,
          updateAssessment.releaseDate,
          updateAssessment.releaseDate,
        )

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `attempting to update both releaseDate and accommodationRequiredFromDate returns validation error`() {
      val assessmentId = UUID.randomUUID()

      val updateAssessment =
        updateAssessmentEntity(
          releaseDate = LocalDate.now(),
          accommodationRequiredFromDate = LocalDate.now(),
        )

      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = assessmentEntity(user)

      every { assessmentRepositoryMock.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Cannot update both dates")
      verify { cas3DomainEventServiceMock wasNot called }
    }

    @Test
    fun `accommodationRequiredFromDate before releaseDate returns validation errors`() {
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = assessmentEntity(user)
      val updateAssessment =
        updateAssessmentEntity(
          releaseDate = null,
          accommodationRequiredFromDate = assessment.releaseDate!!.minusDays(10),
        )

      every { assessmentRepositoryMock.findById(assessmentId) } returns Optional.of(assessment)
      every { assessmentRepositoryMock.save(any()) } returnsArgument 0
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Accommodation required from date cannot be before release date: ${assessment.releaseDate}")
      verify { cas3DomainEventServiceMock wasNot called }
    }

    @Test
    fun `when releaseDate is after accommodationRequiredFromDate returns validation errors`() {
      val assessmentId = UUID.randomUUID()
      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = assessmentEntity(user)
      val updateAssessment =
        updateAssessmentEntity(
          releaseDate = assessment.accommodationRequiredFromDate!!.plusDays(10),
          accommodationRequiredFromDate = null,
        )

      every { assessmentRepositoryMock.findById(any()) } returns Optional.of(assessment)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Release date cannot be after accommodation required from date: ${assessment.accommodationRequiredFromDate}")
      verify { cas3DomainEventServiceMock wasNot called }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "2099-01-01,,2000-01-01",
        ",2000-01-01,2099-01-01",
      ],
    )
    fun `successful update returns expected result, with domain event saved`(
      accommodationDate: LocalDate?,
      releaseDate: LocalDate?,
      existingDate: LocalDate,
    ) {
      val assessmentId = UUID.randomUUID()

      val updateAssessment =
        updateAssessmentEntity(
          releaseDate = releaseDate,
          accommodationRequiredFromDate = accommodationDate,
        )

      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = assessmentEntity(user)
      assessment.releaseDate = existingDate
      assessment.accommodationRequiredFromDate = existingDate

      every { assessmentRepositoryMock.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepositoryMock.save(any()) } returnsArgument 0
      every { cas3DomainEventBuilderMock.buildAssessmentUpdatedDomainEvent(any(), any()) } answers { callOriginal() }
      every { cas3DomainEventServiceMock.saveAssessmentUpdatedEvent(any()) } just Runs

      val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment.releaseDate, updateAssessment.accommodationRequiredFromDate)
      assertThat(result is CasResult.Success).isTrue
      val entity = (result as CasResult.Success).value
      assertThat(entity).isNotNull()
      assertThat(entity.releaseDate).isBefore(entity.accommodationRequiredFromDate)
      verify(exactly = 1) { cas3DomainEventServiceMock.saveAssessmentUpdatedEvent(any()) }
    }

    private fun updateAssessmentEntity(
      releaseDate: LocalDate?,
      accommodationRequiredFromDate: LocalDate?,
    ): UpdateAssessment = UpdateAssessment(
      releaseDate = releaseDate,
      accommodationRequiredFromDate = accommodationRequiredFromDate,
      data = emptyMap(),
    )

    private fun assessmentEntity(user: UserEntity): TemporaryAccommodationAssessmentEntity = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withProbationRegion(user.probationRegion)
          .withCreatedByUser(user)
          .produce(),
      ).withReleaseDate(LocalDate.now().plusDays(5))
      .withAccommodationRequiredFromDate(LocalDate.now().plusDays(5))
      .produce()
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

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(UserEntityFactory().withDefaults().produce())
      .produce()

    @Test
    fun `unauthorised when the user does not have permission to access the assessment`() {
      val assessmentId = UUID.randomUUID()

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns TemporaryAccommodationAssessmentEntityFactory()
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

      val result = assessmentService.rejectAssessment(user, assessmentId, "{}", "reasoning")

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `general validation error for Assessment where assessment has been deallocated`() {
      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(null)
        .withDecision(null)
        .withAllocatedToUser(user)
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessment.id) } returns assessment

      every { offenderServiceMock.getOffenderByCrn(assessment.application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(assessment.application.crn, CaseSummaryFactory().produce())

      val result = assessmentService.rejectAssessment(user, assessment.id, "{}", "reasoning")

      assertThat(result is CasResult.GeneralValidationError).isTrue
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("The application has been reallocated, this assessment is read only")
    }

    @Test
    fun `unauthorised when user not allowed to view Offender (LAO)`() {
      val assessmentId = UUID.randomUUID()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withAllocatedToUser(user)
        .withData("{\"test\": \"data\"}")
        .produce()

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { assessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { temporaryAccommodationAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns PersonSummaryInfoResult.NotFound(assessment.application.crn)

      val result = assessmentService.rejectAssessment(user, assessmentId, "{\"test\": \"data\"}", "reasoning")

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `success, sets completed at timestamp to null`() {
      val assessmentId = UUID.randomUUID()
      val referralRejectionReasonId = UUID.randomUUID()

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
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

      val referralRejectionReason = ReferralRejectionReasonEntityFactory()
        .withId(referralRejectionReasonId)
        .produce()

      every { userAccessServiceMock.userCanViewAssessment(any(), any()) } returns true

      every { temporaryAccommodationAssessmentRepositoryMock.findByIdOrNull(assessmentId) } returns assessment

      every { temporaryAccommodationAssessmentRepositoryMock.save(any()) } answers { it.invocation.args[0] as TemporaryAccommodationAssessmentEntity }

      every { referralRejectionReasonRepositoryMock.findByIdOrNull(referralRejectionReasonId) } returns referralRejectionReason

      every {
        offenderServiceMock.getPersonSummaryInfoResult(
          assessment.application.crn,
          user.cas1LaoStrategy(),
        )
      } returns
        PersonSummaryInfoResult.Success.Full(
          assessment.application.crn,
          CaseSummaryFactory().withCrn(assessment.application.crn).produce(),
        )

      every { userServiceMock.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val result = assessmentService.rejectAssessment(
        user,
        assessmentId,
        "{\"test\": \"data\"}",
        "reasoning",
        referralRejectionReasonId,
        "referral rejection reason detail",
        false,
      )

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success
      val updatedAssessment = result.value
      assertThat(updatedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
      assertThat(updatedAssessment.rejectionRationale).isEqualTo("reasoning")
      assertThat(updatedAssessment.submittedAt).isNotNull()
      assertThat(updatedAssessment.document).isEqualTo("{\"test\": \"data\"}")
      assertThat(updatedAssessment.referralRejectionReason?.id).isEqualTo(referralRejectionReasonId)
      assertThat(updatedAssessment.referralRejectionReasonDetail).isEqualTo("referral rejection reason detail")
      assertThat(updatedAssessment.completedAt).isNull()
      assertAssessmentHasSystemNote(assessment, user, ReferralHistorySystemNoteType.REJECTED)
    }
  }

  @Nested
  inner class Cas3DeallocateAssessment {
    @Test
    fun `deallocateAssessment deallocates an assessment`() {
      val user = UserEntityFactory().withDefaultProbationRegion().produce()
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assigneeUser = UserEntityFactory().withDefaultProbationRegion().produce()
      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      assertThat(assessment.allocatedToUser).isNotNull()
      assertThat(assessment.allocatedAt).isNotNull()
      assertThat(assessment.decision).isNotNull()
      assertThat(assessment.submittedAt).isNotNull()

      every { userAccessServiceMock.userCanDeallocateTask(user) } returns true
      every { temporaryAccommodationAssessmentRepositoryMock.findById(assessment.id) } returns Optional.of(assessment)
      every { assessmentRepositoryMock.save(any()) } returnsArgument 0
      every { userServiceMock.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val result = assessmentService.deallocateAssessment(user, assessment.id)

      assertThat(assessment.allocatedToUser).isNull()
      assertThat(assessment.allocatedAt).isNull()
      assertThat(assessment.decision).isNull()
      assertThat(assessment.submittedAt).isNull()
      assertThat(assessment.referralHistoryNotes).hasSize(1)

      assertThat(result is CasResult.Success).isTrue
      assertThat((result as CasResult.Success).value).isNotNull()
    }
  }

  @Nested
  inner class Cas3ReallocateAssessmentToMe {

    @Test
    fun `reallocateAssessment reallocates an assessment`() {
      val otherUser = UserEntityFactory().withDefaultProbationRegion().produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(otherUser.probationRegion)
        .withCreatedByUser(otherUser)
        .produce()

      val originalAllocationTime = OffsetDateTime.now()
      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(otherUser)
        .withAllocatedAt(originalAllocationTime)
        .produce()

      assertThat(assessment.allocatedToUser).isEqualTo(otherUser)
      assertThat(assessment.allocatedAt).isEqualTo(originalAllocationTime)
      assertThat(assessment.decision).isNotNull()
      assertThat(assessment.referralHistoryNotes).hasSize(0)

      val user = UserEntityFactory().withDefaultProbationRegion().produce()

      every { userAccessServiceMock.userCanReallocateTask(user) } returns true
      every { lockableAssessmentRepositoryMock.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { temporaryAccommodationAssessmentRepositoryMock.findById(assessment.id) } returns Optional.of(assessment)
      every { assessmentRepositoryMock.save(any()) } returnsArgument 0
      every { userServiceMock.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepositoryMock.save(any()) } returnsArgument 0

      val result = assessmentService.reallocateAssessmentToMe(user, assessment.id)

      assertThat(assessment.allocatedToUser).isEqualTo(user)
      assertThat(assessment.allocatedAt).isAfter(originalAllocationTime)
      assertThat(assessment.decision).isNull()
      assertThat(assessment.referralHistoryNotes).hasSize(1)

      assertThat(result is CasResult.Success).isTrue
      assertThat((result as CasResult.Success).value).isNotNull()
    }
  }
}
