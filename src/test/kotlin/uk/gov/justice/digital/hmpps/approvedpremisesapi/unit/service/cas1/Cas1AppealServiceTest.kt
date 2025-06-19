package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class Cas1AppealServiceTest {
  private val appealRepository = mockk<AppealRepository>()
  private val assessmentService = mockk<AssessmentService>()
  private val cas1AppealEmailService = mockk<Cas1AppealEmailService>()
  private val cas1AppealDomainEventService = mockk<Cas1AppealDomainEventService>()

  private val cas1AppealService = Cas1AppealService(
    appealRepository,
    assessmentService,
    cas1AppealEmailService,
    cas1AppealDomainEventService,
  )

  private val probationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private val appealId = UUID.randomUUID()

  @Nested
  inner class GetAppeal {
    @Test
    fun `Returns NotFound if the appeal does not exist`() {
      every { appealRepository.findById(any()) } returns Optional.empty()

      val result = cas1AppealService.getAppeal(UUID.randomUUID(), application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns NotFound if the appeal is not for the given application`() {
      val appeal = AppealEntityFactory()
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = cas1AppealService.getAppeal(appeal.id, application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns Success containing expected appeal`() {
      val appeal = AppealEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = cas1AppealService.getAppeal(appeal.id, application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isEqualTo(appeal)
    }
  }

  @Nested
  inner class CreateAppeal {

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(createdByUser)
      .withStatus(ApprovedPremisesApplicationStatus.REJECTED)
      .produce()

    @Test
    fun `Returns Unauthorised if the creating user does not have the CAS1_APPEALS_MANAGER role`() {
      val result = cas1AppealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThatCasResult(result).isUnauthorised()

      verify { cas1AppealEmailService wasNot Called }
      verify { cas1AppealDomainEventService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(value = ApprovedPremisesApplicationStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["REJECTED"])
    fun `Returns General error if the application state isn't rejected`(status: ApprovedPremisesApplicationStatus) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      application.status = status

      val result = cas1AppealService.createAppeal(
        LocalDate.now().plusDays(1),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThatCasResult(result).isGeneralValidationError("Appeals can only be created for rejected applications")

      verify { cas1AppealEmailService wasNot Called }
      verify { cas1AppealDomainEventService wasNot Called }
    }

    @Test
    fun `Returns FieldValidationError if the appeal date is in the future`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = cas1AppealService.createAppeal(
        LocalDate.now().plusDays(1),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.appealDate", "mustNotBeFuture")

      verify { cas1AppealEmailService wasNot Called }
      verify { cas1AppealDomainEventService wasNot Called }
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the appeal detail is blank`(appealDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = cas1AppealService.createAppeal(
        LocalDate.now(),
        appealDetail,
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.appealDetail", "empty")

      verify { cas1AppealEmailService wasNot Called }
      verify { cas1AppealDomainEventService wasNot Called }
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the decision detail is blank`(decisionDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = cas1AppealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        decisionDetail,
        application,
        assessment,
        createdByUser,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.decisionDetail", "empty")

      verify { cas1AppealEmailService wasNot Called }
      verify { cas1AppealDomainEventService wasNot Called }
    }

    @Test
    fun `Stores appeal in repository and returns the stored appeal`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { cas1AppealDomainEventService.appealRecordCreated(any()) } just Runs
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = cas1AppealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThatCasResult(result).isSuccess().with { appeal ->
          assertThat(appeal).matches {
            it.matches(now)
          }
          verify(exactly = 1) {
            appealRepository.save(appeal)
          }
        }
      }
    }

    @Test
    fun `Does not create a new assessment if the appeal was rejected`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { cas1AppealDomainEventService.appealRecordCreated(any()) } just Runs
      every { cas1AppealEmailService.appealFailed(any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = cas1AppealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.rejected,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThatCasResult(result).isSuccess()

        verify { assessmentService wasNot Called }
        verify(exactly = 0) { cas1AppealEmailService.appealSuccess(any(), any()) }
        verify { cas1AppealDomainEventService.appealRecordCreated(any()) }
      }
    }

    @Test
    fun `Sends a failure email if the appeal was rejected`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { cas1AppealDomainEventService.appealRecordCreated(any()) } just Runs
      every { cas1AppealEmailService.appealFailed(any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = cas1AppealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.rejected,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThatCasResult(result).isSuccess()

        verify(exactly = 1) { cas1AppealEmailService.appealFailed(application) }
        verify(exactly = 0) { cas1AppealEmailService.appealSuccess(any(), any()) }
        verify { cas1AppealDomainEventService.appealRecordCreated(any()) }
      }
    }

    @Test
    fun `Creates a new assessment if the appeal was accepted and triggers emails and domain events`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { cas1AppealDomainEventService.appealRecordCreated(any()) } just Runs
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = cas1AppealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThatCasResult(result).isSuccess()

        verify(exactly = 1) {
          assessmentService.createApprovedPremisesAssessment(application, createdFromAppeal = true)
        }

        verify(exactly = 1) {
          cas1AppealEmailService.appealSuccess(
            application,
            match { it.application == application && it.createdBy == createdByUser },
          )
        }
        verify(exactly = 0) { cas1AppealEmailService.appealFailed(any()) }

        verify(exactly = 1) {
          cas1AppealDomainEventService.appealRecordCreated(
            match { it.application == application && it.createdBy == createdByUser },
          )
        }
      }
    }

    private fun AppealEntity.matches(now: LocalDate) = this.id == appealId &&
      this.appealDate == now &&
      this.appealDetail == "Some information about why the appeal is being made" &&
      this.decision == AppealDecision.accepted.value &&
      this.decisionDetail == "Some information about the decision made" &&
      this.application == application &&
      this.assessment == assessment &&
      this.createdBy == createdByUser
  }
}
