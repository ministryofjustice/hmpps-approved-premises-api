package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.Runs
import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3AssessmentServiceTest {
  @MockK
  lateinit var assessmentRepository: AssessmentRepository

  @MockK
  lateinit var temporaryAccommodationAssessmentRepository: TemporaryAccommodationAssessmentRepository

  @MockK
  lateinit var lockableAssessmentRepository: LockableAssessmentRepository

  @MockK
  lateinit var assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository

  @MockK
  lateinit var userAccessService: UserAccessService

  @MockK
  lateinit var userService: UserService

  @MockK
  lateinit var cas3DomainEventService: Cas3DomainEventService

  @MockK
  lateinit var cas3DomainEventBuilder: Cas3DomainEventBuilder

  @MockK
  lateinit var offenderService: OffenderService

  @InjectMockKs
  lateinit var assessmentService: Cas3AssessmentService

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

      every { userAccessService.userCanViewAssessment(user, assessment) } returns true

      every { temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
      every {
        offenderService.getOffenderByCrn(
          assessment.application.crn,
          user.deliusUsername,
        )
      } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every { offenderService.getPersonSummaryInfoResult(assessment.application.crn, user.cas1LaoStrategy()) } returns
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

      every { userAccessService.userCanViewAssessment(user, assessment) } returns false

      every { temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId) } returns assessment

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

      every { temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId) } returns null

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
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          any(),
          null,
          emptyList(),
          pageRequest,
        )
      } returns Page.empty()

      assessmentService.getAssessmentSummariesForUser(user, null, emptyList(), pageCriteria)

      verify(exactly = 1) {
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
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
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
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
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
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
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
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
        assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
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

      every { assessmentRepository.findById(assessmentId) } returns (Optional.empty())

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

      every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessService.userCanViewAssessment(any(), any()) } returns false

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

      every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessService.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Cannot update both dates")
      verify { cas3DomainEventService wasNot called }
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

      every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
      every { assessmentRepository.save(any()) } returnsArgument 0
      every { userAccessService.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Accommodation required from date cannot be before release date: ${assessment.releaseDate}")
      verify { cas3DomainEventService wasNot called }
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

      every { assessmentRepository.findById(any()) } returns Optional.of(assessment)
      every { userAccessService.userCanViewAssessment(any(), any()) } returns true

      val result = assessmentService.updateAssessment(
        user,
        assessmentId,
        updateAssessment.releaseDate,
        updateAssessment.accommodationRequiredFromDate,
      ) as CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>
      assertThat(result.message).isEqualTo("Release date cannot be after accommodation required from date: ${assessment.accommodationRequiredFromDate}")
      verify { cas3DomainEventService wasNot called }
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

      every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
      every { userAccessService.userCanViewAssessment(any(), any()) } returns true
      every { assessmentRepository.save(any()) } returnsArgument 0
      every { cas3DomainEventBuilder.buildAssessmentUpdatedDomainEvent(any(), any()) } answers { callOriginal() }
      every { cas3DomainEventService.saveAssessmentUpdatedEvent(any()) } just Runs

      val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment.releaseDate, updateAssessment.accommodationRequiredFromDate)
      assertThat(result is CasResult.Success).isTrue
      val entity = (result as CasResult.Success).value
      assertThat(entity).isNotNull()
      assertThat(entity.releaseDate).isBefore(entity.accommodationRequiredFromDate)
      verify(exactly = 1) { cas3DomainEventService.saveAssessmentUpdatedEvent(any()) }
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

      every { userAccessService.userCanDeallocateTask(user) } returns true
      every { temporaryAccommodationAssessmentRepository.findById(assessment.id) } returns Optional.of(assessment)
      every { assessmentRepository.save(any()) } returnsArgument 0
      every { userService.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepository.save(any()) } returnsArgument 0

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

      every { userAccessService.userCanReallocateTask(user) } returns true
      every { lockableAssessmentRepository.acquirePessimisticLock(assessment.id) } returns LockableAssessmentEntity(assessment.id)
      every { temporaryAccommodationAssessmentRepository.findById(assessment.id) } returns Optional.of(assessment)
      every { assessmentRepository.save(any()) } returnsArgument 0
      every { userService.getUserForRequest() } returns user
      every { assessmentReferralHistoryNoteRepository.save(any()) } returnsArgument 0

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
