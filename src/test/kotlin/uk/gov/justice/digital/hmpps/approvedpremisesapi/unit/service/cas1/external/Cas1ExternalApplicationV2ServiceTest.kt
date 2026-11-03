package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ExternalPlacementV2DtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ExternalRequestForPlacementDtoV2Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationV2Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1RequestForPlacementOutcomeV2Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1RequestForPlacementOutcomeV2Service.RequestForPlacementOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationV2Transformer
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class Cas1ExternalApplicationV2ServiceTest {
  @MockK
  private lateinit var repository: ApprovedPremisesApplicationRepository

  @MockK
  private lateinit var outcomeService: Cas1RequestForPlacementOutcomeV2Service

  @MockK
  private lateinit var transformer: Cas1ExternalApplicationV2Transformer

  @MockK
  private lateinit var transformed: Cas1ExternalApplicationV2Dto

  private val today = LocalDate.of(2026, 6, 15)
  private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

  private lateinit var service: Cas1ExternalApplicationV2Service

  private val user = UserEntityFactory().withDefaults().produce()
  private val crn = "X12345"

  @BeforeEach
  fun setup() {
    service = Cas1ExternalApplicationV2Service(repository, outcomeService, transformer, clock)
  }

  @Nested
  inner class ChoosingSuitableApplication {
    @Test
    fun `returns null when no applications exist`() {
      every { repository.findByCrn(crn) } returns emptyList()

      assertThat(service.getSuitableApplicationByCrn(crn)).isNull()
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external.Cas1ExternalApplicationV2ServiceTest#suitableApplicationScenarioArguments")
    fun `returns application with most advanced status`(
      applications: List<ApprovedPremisesApplicationEntity>,
      expectedSuitableApplication: ApprovedPremisesApplicationEntity,
    ) {
      every { repository.findByCrn(crn) } returns applications
      every { outcomeService.getOutcomes(expectedSuitableApplication.id) } returns emptyList()
      every { transformer.transformToApplicationV2(expectedSuitableApplication, null, emptyList()) } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)
    }

    @Test
    fun `returns most recently submitted application when statuses match`() {
      val older = application(ApprovedPremisesApplicationStatus.REJECTED, OffsetDateTime.parse("2026-01-01T00:00:00Z"))
      val newer = application(ApprovedPremisesApplicationStatus.REJECTED, OffsetDateTime.parse("2026-02-01T00:00:00Z"))

      every { repository.findByCrn(crn) } returns listOf(newer, older)
      every { outcomeService.getOutcomes(newer.id) } returns emptyList()
      every { transformer.transformToApplicationV2(newer, null, emptyList()) } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)
    }
  }

  @Nested
  inner class ReturningOutcomes {
    private val application = application(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)

    @Test
    fun `has no upcoming request when no placement is pending`() {
      val arrived = outcome(
        statusLastUpdated = today.minusDays(20),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.ARRIVED,
        placementExpectedArrival = today.minusDays(20),
        placementExpectedDeparture = today.minusDays(10),
      )
      val noPlacement = outcome(
        statusLastUpdated = today.minusDays(3),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = null,
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(arrived, noPlacement)
      every { transformer.transformToApplicationV2(application, null, listOf(noPlacement.toDto(), arrived.toDto())) } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)
    }

    @Test
    fun `uses earliest upcoming placement as upcoming and excludes all pending arrivals from history, newest first`() {
      val laterUpcoming = outcome(
        statusLastUpdated = today.minusDays(5),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today.plusDays(30),
        placementExpectedDeparture = today.plusDays(40),
      )
      val earliestUpcoming = outcome(
        statusLastUpdated = today.minusDays(9),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today,
        placementExpectedDeparture = today.plusDays(1),
      )
      val pastUpcoming = outcome(
        statusLastUpdated = today.minusDays(1),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today.minusDays(1),
        placementExpectedDeparture = today,
      )
      val arrived = outcome(
        statusLastUpdated = today.minusDays(20),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.ARRIVED,
        placementExpectedArrival = today.minusDays(20),
        placementExpectedDeparture = today.minusDays(10),
      )
      val noPlacement = outcome(
        statusLastUpdated = today.minusDays(3),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = null,
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(arrived, laterUpcoming, noPlacement, earliestUpcoming, pastUpcoming)
      every {
        transformer.transformToApplicationV2(
          application,
          earliestUpcoming.toDto(),
          listOf(pastUpcoming.toDto(), noPlacement.toDto(), arrived.toDto()),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)
    }
  }

  private fun application(status: ApprovedPremisesApplicationStatus, submittedAt: OffsetDateTime? = null) = ApprovedPremisesApplicationEntityFactory()
    .withDefaults()
    .withCreatedByUser(user)
    .withStatus(status)
    .withSubmittedAt(submittedAt)
    .produce()

  private fun RequestForPlacementOutcome.toDto() = Cas1ExternalRequestForPlacementOutcomeDto(requestForPlacement, placement)

  private fun outcome(
    statusLastUpdated: LocalDate,
    requestForPlacementStatus: RequestForPlacementStatus,
    placementStatus: Cas1SpaceBookingStatus? = null,
    placementExpectedArrival: LocalDate? = null,
    placementExpectedDeparture: LocalDate? = null,
  ) = RequestForPlacementOutcome(
    requestForPlacement = Cas1ExternalRequestForPlacementDtoV2Factory()
      .withStatus(requestForPlacementStatus)
      .produce(),
    placement = placementStatus?.let {
      Cas1ExternalPlacementV2DtoFactory()
        .withStatus(it)
        .withExpectedArrivalDate(placementExpectedArrival!!)
        .withExpectedDepartureDate(placementExpectedDeparture!!)
        .produce()
    },
    statusLastUpdated = statusLastUpdated,
  )

  companion object {
    private val statusesByPriority = listOf(
      ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
      ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
      ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
      ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION,
      ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS,
      ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT,
      ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT,
      ApprovedPremisesApplicationStatus.STARTED,
      ApprovedPremisesApplicationStatus.REJECTED,
      ApprovedPremisesApplicationStatus.WITHDRAWN,
      ApprovedPremisesApplicationStatus.EXPIRED,
      ApprovedPremisesApplicationStatus.INAPPLICABLE,
    )

    /**
     * For each status, the application with that status is expected to be chosen
     * when all applications with that status or lower priority exist
     */
    @JvmStatic
    fun suitableApplicationScenarioArguments(): Stream<Arguments> {
      val user = UserEntityFactory().withDefaults().produce()
      val applicationsByPriority = statusesByPriority.map { status ->
        ApprovedPremisesApplicationEntityFactory()
          .withDefaults()
          .withCreatedByUser(user)
          .withStatus(status)
          .produce()
      }

      return applicationsByPriority.indices.map { index ->
        Arguments.of(applicationsByPriority.drop(index).reversed(), applicationsByPriority[index])
      }.stream()
    }
  }
}
