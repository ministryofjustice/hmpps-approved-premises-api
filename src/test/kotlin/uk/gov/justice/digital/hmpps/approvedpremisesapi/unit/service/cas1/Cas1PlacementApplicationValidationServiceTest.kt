package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.PlacementApplicationDecisionEnvelopeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PlacementApplicationValidationServiceTest {

  @MockK
  private lateinit var placementApplicationRepository: PlacementApplicationRepository

  @InjectMockKs
  private lateinit var service: Cas1PlacementApplicationValidationService

  @Nested
  inner class ValidateDecision {
    val allocatedToUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val createdByUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `Return not found if can't be found`() {
      val id = UUID.randomUUID()
      every { placementApplicationRepository.findByIdOrNull(id) } returns null

      val result = service.validateDecision(
        id,
        PlacementApplicationDecisionEnvelopeFactory().produce(),
        allocatedToUser,
      )

      assertThatCasResult(result).isNotFound("PlacementApplication", id)
    }

    @Test
    fun `Return error if calling user is not the allocated user`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withAllocatedToUser(createdByUser)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory().produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `Return error if a decision has already been set`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withAllocatedToUser(allocatedToUser)
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withCreatedByUser(createdByUser)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory().produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isGeneralValidationError("This application has already had a decision set")
    }

    @ParameterizedTest
    @EnumSource(
      value = PlacementApplicationDecisionDto::class,
      names = ["withdraw", "withdrawnByPp"],
      mode = EnumSource.Mode.INCLUDE,
    )
    fun `Rejecting with withdrawal reasons errors`(decision: PlacementApplicationDecisionDto) {
      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withAllocatedToUser(allocatedToUser)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(decision)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isGeneralValidationError("Decision $decision is not supported")
    }

    @Test
    fun `If valid return success`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withAllocatedToUser(allocatedToUser)
        .withDecision(null)
        .withCreatedByUser(createdByUser)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory().produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.placementApplication).isEqualTo(placementApplication)
      }
    }
  }
}
