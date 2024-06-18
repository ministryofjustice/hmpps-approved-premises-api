package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3AssessmentService
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3AssessmentServiceTest {

  @MockK
  lateinit var assessmentRepository: AssessmentRepository

  @MockK
  lateinit var userAccessService: UserAccessService

  @InjectMockKs
  lateinit var assessmentService: Cas3AssessmentService

  @Test
  fun `an invalid assessment id returns a validation error`() {
    val assessmentId = UUID.randomUUID()
    val updateAssessment = updateAssessmentEntity(releaseDate = LocalDate.now(), accommodationRequiredFromDate = null)
    val user = UserEntityFactory().withDefaultProbationRegion().produce()

    every { assessmentRepository.findById(assessmentId) } returns (Optional.empty())

    val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment)
    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `user unable to access assessment returns unauthorised validation error`() {
    val assessmentId = UUID.randomUUID()

    val updateAssessment = updateAssessmentEntity(
      releaseDate = LocalDate.now(),
      accommodationRequiredFromDate = null,
    )

    val user = UserEntityFactory().withDefaultProbationRegion().produce()
    val assessment = assessmentEntity(user)

    every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
    every { userAccessService.userCanViewAssessment(any(), any()) } returns false

    val result = assessmentService.updateAssessment(
      user,
      assessmentId,
      updateAssessment,
    )

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `attempting to update both releaseDate and accommodationRequiredFromDate returns validation error`() {
    val assessmentId = UUID.randomUUID()

    val updateAssessment = updateAssessmentEntity(
      releaseDate = LocalDate.now(),
      accommodationRequiredFromDate = LocalDate.now(),
    )

    val user = UserEntityFactory().withDefaultProbationRegion().produce()
    val assessment = assessmentEntity(user)

    every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
    every { userAccessService.userCanViewAssessment(any(), any()) } returns true

    val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertValidationMessage(result, "Cannot update both dates")
  }

  @Test
  fun `accommodationRequiredFromDate before releaseDate returns validation errors`() {
    val assessmentId = UUID.randomUUID()

    val user = UserEntityFactory().withDefaultProbationRegion().produce()
    val assessment = assessmentEntity(user)
    val updateAssessment = updateAssessmentEntity(
      releaseDate = null,
      accommodationRequiredFromDate = assessment.releaseDate!!.minusDays(10),
    )

    every { assessmentRepository.findById(assessmentId) } returns Optional.of(assessment)
    every { assessmentRepository.save(any()) } returnsArgument 0
    every { userAccessService.userCanViewAssessment(any(), any()) } returns true

    val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertValidationMessage(result, "Accommodation required from date cannot be before release date: ${assessment.releaseDate}")
  }

  @Test
  fun `when releaseDate is after accommodationRequiredFromDate returns validation errors`() {
    val assessmentId = UUID.randomUUID()
    val user = UserEntityFactory().withDefaultProbationRegion().produce()
    val assessment = assessmentEntity(user)
    val updateAssessment = updateAssessmentEntity(
      releaseDate = assessment.accommodationRequiredFromDate!!.plusDays(10),
      accommodationRequiredFromDate = null,
    )

    every { assessmentRepository.findById(any()) } returns Optional.of(assessment)
    every { userAccessService.userCanViewAssessment(any(), any()) } returns true

    val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertValidationMessage(
      result,
      "Release date cannot be after accommodation required from date: ${assessment.accommodationRequiredFromDate}",
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "2099-01-01,,2000-01-01",
      ",2000-01-01,2099-01-01",
    ],
  )
  fun `successful update returns expected result`(
    accommodationDate: LocalDate?,
    releaseDate: LocalDate?,
    existingDate: LocalDate,
  ) {
    val assessmentId = UUID.randomUUID()

    val updateAssessment = updateAssessmentEntity(
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

    val result = assessmentService.updateAssessment(user, assessmentId, updateAssessment)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    val entity =
      (((result as AuthorisableActionResult.Success).entity) as ValidatableActionResult.Success<TemporaryAccommodationAssessmentEntity>).entity
    assertThat(entity).isNotNull()
    assertThat(entity.releaseDate).isBefore(entity.accommodationRequiredFromDate)
  }

  private fun assertValidationMessage(
    result: AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>>,
    message: String,
  ) {
    assertThat(
      (
        (result as AuthorisableActionResult.Success)
          .entity as ValidatableActionResult.GeneralValidationError
        )
        .message,
    ).isEqualTo(message)
  }

  private fun updateAssessmentEntity(
    releaseDate: LocalDate?,
    accommodationRequiredFromDate: LocalDate?,
  ): UpdateAssessment {
    return UpdateAssessment(
      releaseDate = releaseDate,
      accommodationRequiredFromDate = accommodationRequiredFromDate,
      data = emptyMap(),
    )
  }

  private fun assessmentEntity(user: UserEntity): TemporaryAccommodationAssessmentEntity {
    return TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withProbationRegion(user.probationRegion)
          .withCreatedByUser(user).produce(),
      )
      .withReleaseDate(LocalDate.now().plusDays(5))
      .withAccommodationRequiredFromDate(LocalDate.now().plusDays(5))
      .produce()
  }
}
