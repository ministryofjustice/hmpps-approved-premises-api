package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ExternalPlacementV2DtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ExternalRequestForPlacementDtoV2Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1RequestForPlacementOutcomeV2Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1RequestForPlacementOutcomeV2Service.RequestForPlacementOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalRequestForPlacementOutcomeV2Transformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1RequestForPlacementOutcomeV2ServiceTest {
  @MockK
  private lateinit var cas1RequestForPlacementService: Cas1RequestForPlacementService

  @MockK
  private lateinit var cas1PremisesService: Cas1PremisesService

  @MockK
  private lateinit var transformer: Cas1ExternalRequestForPlacementOutcomeV2Transformer

  @MockK
  private lateinit var spaceBookingManagementService: Cas1SpaceBookingManagementService

  @MockK
  private lateinit var spaceBookingTransformer: Cas1SpaceBookingTransformer

  @InjectMockKs
  private lateinit var service: Cas1RequestForPlacementOutcomeV2Service

  private val applicationId = UUID.randomUUID()
  private val premises = ApprovedPremisesEntityFactory().withDefaults().produce()

  @Nested
  inner class GetOutcomes {

    @Test
    fun `returns empty list when there are no requests for placement`() {
      every {
        cas1RequestForPlacementService.getRequestsForPlacementByApplication(
          applicationId,
          null,
        )
      } returns CasResult.Success(emptyList())

      assertThat(service.getOutcomes(applicationId)).isEmpty()
    }

    @Test
    fun `uses request status date when request has no placements`() {
      val rfp = RequestForPlacementFactory().withStatusSetDate(LocalDate.of(2026, 1, 2)).produce()
      val rfpDto = rfpDto()

      every {
        cas1RequestForPlacementService.getRequestsForPlacementByApplication(
          applicationId,
          null,
        )
      } returns CasResult.Success(listOf(rfp))
      every { transformer.transformToRequestForPlacement(rfp) } returns rfpDto

      assertThat(service.getOutcomes(applicationId)).containsExactly(
        RequestForPlacementOutcome(
          rfpDto,
          null,
          LocalDate.of(2026, 1, 2),
        ),
      )
    }

    @Test
    fun `returns one outcome per placement using placement status date`() {
      val placement1 = placement(LocalDate.of(2026, 3, 1))
      val placement2 = placement(LocalDate.of(2026, 4, 1))
      val rfp = RequestForPlacementFactory()
        .withStatusSetDate(LocalDate.of(2026, 1, 2))
        .withPlacements(listOf(placement1, placement2))
        .produce()
      val rfpDto = rfpDto()
      val placementDto1 = placementDto()
      val placementDto2 = placementDto()

      every {
        cas1RequestForPlacementService.getRequestsForPlacementByApplication(
          applicationId,
          null,
        )
      } returns CasResult.Success(listOf(rfp))
      every { cas1PremisesService.findPremisesById(premises.id) } returns premises
      every { transformer.transformToRequestForPlacement(rfp) } returns rfpDto
      every { transformer.transformToPlacement(placement1, premises) } returns placementDto1
      every { transformer.transformToPlacement(placement2, premises) } returns placementDto2

      assertThat(service.getOutcomes(applicationId)).containsExactly(
        RequestForPlacementOutcome(rfpDto, placementDto1, LocalDate.of(2026, 3, 1)),
        RequestForPlacementOutcome(rfpDto, placementDto2, LocalDate.of(2026, 4, 1)),
      )
    }

    @Test
    fun `handles a mix of requests with and without placements`() {
      val placement = placement(LocalDate.of(2026, 3, 1))
      val rfpWithPlacement = RequestForPlacementFactory().withPlacements(listOf(placement)).produce()
      val rfpWithout = RequestForPlacementFactory().withStatusSetDate(LocalDate.of(2026, 2, 1)).produce()
      val rfpDtoWith = rfpDto()
      val placementDtoWith = placementDto()
      val rfpDtoWithout = rfpDto()

      every {
        cas1RequestForPlacementService.getRequestsForPlacementByApplication(
          applicationId,
          null,
        )
      } returns CasResult.Success(listOf(rfpWithPlacement, rfpWithout))
      every { cas1PremisesService.findPremisesById(premises.id) } returns premises
      every { transformer.transformToRequestForPlacement(rfpWithPlacement) } returns rfpDtoWith
      every { transformer.transformToPlacement(placement, premises) } returns placementDtoWith
      every { transformer.transformToRequestForPlacement(rfpWithout) } returns rfpDtoWithout

      assertThat(service.getOutcomes(applicationId)).containsExactly(
        RequestForPlacementOutcome(rfpDtoWith, placementDtoWith, LocalDate.of(2026, 3, 1)),
        RequestForPlacementOutcome(rfpDtoWithout, null, LocalDate.of(2026, 2, 1)),
      )
    }

    private fun rfpDto() = Cas1ExternalRequestForPlacementDtoV2Factory().produce()

    private fun placementDto() = Cas1ExternalPlacementV2DtoFactory().produce()

    private fun placement(statusSetDate: LocalDate) = Cas1SpaceBookingShortSummaryFactory()
      .withPremises(NamedId(premises.id, premises.name))
      .withStatusSetDate(statusSetDate)
      .produce()
  }

  @Nested
  inner class GetCurrentResidentPlacement {

    @Test
    fun `returns null when the person has no current residency`() {
      every { spaceBookingManagementService.findCurrentResidency("CRN123") } returns null

      assertThat(service.getCurrentResidentPlacement("CRN123")).isNull()
    }

    @Test
    fun `returns the transformed placement for the current residency`() {
      val booking = Cas1SpaceBookingEntityFactory().withPremises(premises).produce()
      val placementDto = Cas1ExternalPlacementV2DtoFactory().produce()

      every { spaceBookingManagementService.findCurrentResidency("CRN123") } returns booking
      every { transformer.transformToPlacement(booking) } returns placementDto

      assertThat(service.getCurrentResidentPlacement("CRN123")).isEqualTo(placementDto)
    }
  }
}
