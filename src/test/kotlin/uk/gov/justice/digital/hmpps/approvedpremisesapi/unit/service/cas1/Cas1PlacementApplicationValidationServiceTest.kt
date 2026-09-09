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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementApplicationDecisionAcceptanceDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1AuthorisedPlacementPeriodFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.PlacementApplicationDecisionEnvelopeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationValidationService.ValidatedDecision.ValidatedAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationValidationService.ValidatedDecision.ValidatedRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
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

    val baselinePlacementApplication = PlacementApplicationEntityFactory()
      .withDefaults()
      .withAllocatedToUser(allocatedToUser)
      .withDecision(null)
      .withCreatedByUser(createdByUser)

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
      val placementApplication = baselinePlacementApplication
        .withAllocatedToUser(createdByUser)
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
      val placementApplication = baselinePlacementApplication
        .withDecision(PlacementApplicationDecision.ACCEPTED)
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
      val placementApplication = baselinePlacementApplication
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
    fun `Cannot change arrival date on acceptance`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(5)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(
          Cas1PlacementApplicationDecisionAcceptanceDto(
            Cas1AuthorisedPlacementPeriodFactory()
              .withArrival(LocalDate.of(2014, 1, 3))
              .withArrivalFlexible(true)
              .withDuration(5)
              .produce(),
          ),
        )
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isGeneralValidationError("Cannot change arrival date")
    }

    @Test
    fun `Cannot change arrival flexible on acceptance`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(5)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(
          Cas1PlacementApplicationDecisionAcceptanceDto(
            Cas1AuthorisedPlacementPeriodFactory()
              .withArrival(LocalDate.of(2014, 1, 2))
              .withArrivalFlexible(false)
              .withDuration(5)
              .produce(),
          ),
        )
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isGeneralValidationError("Cannot change the value set for 'arrivalFlexible'")
    }

    @Test
    fun `Cannot change requested duration on acceptance, if set`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(5)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(
          Cas1PlacementApplicationDecisionAcceptanceDto(
            Cas1AuthorisedPlacementPeriodFactory()
              .withArrival(LocalDate.of(2014, 1, 2))
              .withArrivalFlexible(true)
              .withDuration(10)
              .produce(),
          ),
        )
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isGeneralValidationError("Authorised duration must match the requested duration")
    }

    @Test
    fun `If valid acceptance return success with authorised placement period, with non-null requested duration`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(5)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(
          Cas1PlacementApplicationDecisionAcceptanceDto(
            Cas1AuthorisedPlacementPeriodFactory()
              .withArrival(LocalDate.of(2014, 1, 2))
              .withArrivalFlexible(true)
              .withDuration(5)
              .produce(),
          ),
        )
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isInstanceOf(ValidatedAcceptance::class.java)
        it as ValidatedAcceptance
        assertThat(it.placementApplication).isEqualTo(placementApplication)
        assertThat(it.authorisedPlacementPeriod.arrival).isEqualTo(LocalDate.of(2014, 1, 2))
        assertThat(it.authorisedPlacementPeriod.arrivalFlexible).isTrue()
        assertThat(it.authorisedPlacementPeriod.duration).isEqualTo(5)
      }
    }

    @Test
    fun `If valid acceptance return success with authorised placement period, with non-null requested duration, no acceptance provided (legacy scenario)`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(5)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(null)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isInstanceOf(ValidatedAcceptance::class.java)
        it as ValidatedAcceptance
        assertThat(it.placementApplication).isEqualTo(placementApplication)
        assertThat(it.authorisedPlacementPeriod.arrival).isEqualTo(LocalDate.of(2014, 1, 2))
        assertThat(it.authorisedPlacementPeriod.arrivalFlexible).isTrue()
        assertThat(it.authorisedPlacementPeriod.duration).isEqualTo(5)
      }
    }

    @Test
    fun `If valid acceptance return success with authorised placement period, with null requested duration`() {
      val placementApplication = baselinePlacementApplication
        .withExpectedArrival(LocalDate.of(2014, 1, 2))
        .withExpectedArrivalFlexible(true)
        .withRequestedDuration(null)
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.accepted)
        .withAcceptance(
          Cas1PlacementApplicationDecisionAcceptanceDto(
            Cas1AuthorisedPlacementPeriodFactory()
              .withArrival(LocalDate.of(2014, 1, 2))
              .withDuration(10)
              .produce(),
          ),
        )
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isInstanceOf(ValidatedAcceptance::class.java)
        assertThat(it.placementApplication).isEqualTo(placementApplication)
        it as ValidatedAcceptance
        assertThat(it.placementApplication).isEqualTo(placementApplication)
        assertThat(it.authorisedPlacementPeriod.arrival).isEqualTo(LocalDate.of(2014, 1, 2))
        assertThat(it.authorisedPlacementPeriod.arrivalFlexible).isTrue()
        assertThat(it.authorisedPlacementPeriod.duration).isEqualTo(10)
      }
    }

    @Test
    fun `If valid rejection return success`() {
      val placementApplication = baselinePlacementApplication
        .produce()

      val placementApplicationDecisionEnvelope = PlacementApplicationDecisionEnvelopeFactory()
        .withDecision(PlacementApplicationDecisionDto.rejected)
        .produce()

      every { placementApplicationRepository.findByIdOrNull(placementApplication.id) } returns placementApplication

      val result = service.validateDecision(
        placementApplication.id,
        placementApplicationDecisionEnvelope,
        allocatedToUser,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isInstanceOf(ValidatedRejection::class.java)
        assertThat(it.placementApplication).isEqualTo(placementApplication)
      }
    }
  }
}
