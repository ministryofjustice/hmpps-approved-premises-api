package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class AppealServiceTest {
  private val appealRepository = mockk<AppealRepository>()

  private val appealService = AppealService(appealRepository)

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

  @Nested
  inner class GetAppeal {
    @Test
    fun `Returns Unauthorised if the user does not have the CAS1_APPEALS_MANAGER role`() {
      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val appeal = AppealEntityFactory()
        .withApplication(application)
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = appealService.getAppeal(appeal.id, application, user)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Unauthorised::class.java)
    }

    @Test
    fun `Returns NotFound if the appeal does not exist`() {
      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()
        .addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      every { appealRepository.findById(any()) } returns Optional.empty()

      val result = appealService.getAppeal(UUID.randomUUID(), application, user)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns NotFound if the appeal is not for the given application`() {
      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()
        .addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val appeal = AppealEntityFactory()
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = appealService.getAppeal(appeal.id, application, user)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns Success containing expected appeal`() {
      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()
        .addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val appeal = AppealEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = appealService.getAppeal(appeal.id, application, user)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isEqualTo(appeal)
    }
  }

  @Nested
  inner class CreateAppeal {
    @Test
    fun `Returns Unauthorised if the creating user does not have the CAS1_APPEALS_MANAGER role`() {
      val result = appealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        "ReviewBot 9000",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Unauthorised::class.java)
    }

    @Test
    fun `Returns FieldValidationError if the appeal date is in the future`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now().plusDays(1),
        "Some information about why the appeal is being made",
        "ReviewBot 9000",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.appealDate", "mustNotBeFuture")
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the appeal detail is blank`(appealDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now(),
        appealDetail,
        "ReviewBot 9000",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.appealDetail", "empty")
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the reviewer is blank`(reviewer: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        reviewer,
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.reviewer", "empty")
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the decision detail is blank`(decisionDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        "ReviewBot 9000",
        AppealDecision.accepted,
        decisionDetail,
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.decisionDetail", "empty")
    }

    @Test
    fun `Stores appeal in repository and returns the stored appeal`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0

      val result = appealService.createAppeal(
        now,
        "Some information about why the appeal is being made",
        "ReviewBot 9000",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
      val resultEntity = result.entity as ValidatableActionResult.Success
      assertThat(resultEntity.entity).matches {
        it.matches(now)
      }
      verify(exactly = 1) {
        appealRepository.save(
          match {
            it.matches(now)
          },
        )
      }
    }

    private fun AppealEntity.matches(now: LocalDate) =
      this.appealDate == now &&
        this.appealDetail == "Some information about why the appeal is being made" &&
        this.reviewer == "ReviewBot 9000" &&
        this.decision == AppealDecision.accepted.value &&
        this.decisionDetail == "Some information about the decision made" &&
        this.application == application &&
        this.assessment == assessment &&
        this.createdBy == createdByUser
  }
}
