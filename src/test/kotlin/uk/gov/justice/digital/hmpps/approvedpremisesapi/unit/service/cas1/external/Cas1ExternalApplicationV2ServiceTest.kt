package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
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

    @EnumSource(
      value = RequestForPlacementStatus::class,
      names = ["awaitingMatch", "requestSubmitted"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    @ParameterizedTest
    fun `has no upcoming request for placement outcome when request for placement is not in a pending state, with no placement`(
      rfpStatus: RequestForPlacementStatus,
    ) {
      val noPlacement = outcome(
        statusLastUpdated = today.minusDays(3),
        requestForPlacementStatus = rfpStatus,
        placementStatus = null,
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(noPlacement)

      val upcomingRequestForPlacementSlot = slot<Cas1ExternalRequestForPlacementOutcomeDto?>()
      val requestForPlacementHistorySlot = slot<List<Cas1ExternalRequestForPlacementOutcomeDto>>()
      every {
        transformer.transformToApplicationV2(
          application = application,
          upcomingRequestForPlacement = captureNullable(upcomingRequestForPlacementSlot),
          requestForPlacementHistory = capture(requestForPlacementHistorySlot),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)

      assertThat(upcomingRequestForPlacementSlot.captured).isNull()
      assertThat(requestForPlacementHistorySlot.captured).isEqualTo(listOf(noPlacement.toDto()))
    }

    @EnumSource(
      value = RequestForPlacementStatus::class,
      names = ["awaitingMatch", "requestSubmitted"],
      mode = EnumSource.Mode.INCLUDE,
    )
    @ParameterizedTest
    fun `upcoming requests for placement outcome set to request for placement when pending outcome, with no placement`(
      rfpStatus: RequestForPlacementStatus,
    ) {
      val pending = outcome(
        statusLastUpdated = today.minusDays(3),
        requestForPlacementStatus = rfpStatus,
        requestForPlacementExpectedArrival = today.plusDays(25),
        placementStatus = null,
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(pending)

      val upcomingRequestForPlacementSlot = slot<Cas1ExternalRequestForPlacementOutcomeDto?>()
      val requestForPlacementHistorySlot = slot<List<Cas1ExternalRequestForPlacementOutcomeDto>>()
      every {
        transformer.transformToApplicationV2(
          application = application,
          upcomingRequestForPlacement = captureNullable(upcomingRequestForPlacementSlot),
          requestForPlacementHistory = capture(requestForPlacementHistorySlot),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)

      assertThat(upcomingRequestForPlacementSlot.captured).isEqualTo(pending.toDto())
      assertThat(requestForPlacementHistorySlot.captured).isEmpty()
    }

    @Test
    fun `has no upcoming request for placement outcome when request for placement is not pending`() {
      val arrived = outcome(
        statusLastUpdated = today.minusDays(20),
        requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
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

      val upcomingRequestForPlacementSlot = slot<Cas1ExternalRequestForPlacementOutcomeDto?>()
      val requestForPlacementHistorySlot = slot<List<Cas1ExternalRequestForPlacementOutcomeDto>>()
      every {
        transformer.transformToApplicationV2(
          application = application,
          upcomingRequestForPlacement = captureNullable(upcomingRequestForPlacementSlot),
          requestForPlacementHistory = capture(requestForPlacementHistorySlot),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)

      assertThat(upcomingRequestForPlacementSlot.captured).isNull()
      assertThat(requestForPlacementHistorySlot.captured).isEqualTo(listOf(noPlacement.toDto(), arrived.toDto()))
    }

    @Test
    fun `uses earliest upcoming placement as upcoming and excludes all pending arrivals from history, newest first`() {
      val laterUpcomingPlacement = outcome(
        statusLastUpdated = today.minusDays(5),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        requestForPlacementExpectedArrival = today.plusDays(25),
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today.plusDays(30),
        placementExpectedDeparture = today.plusDays(40),
      )
      val earliestUpcomingPlacement = outcome(
        statusLastUpdated = today.minusDays(9),
        requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today,
        placementExpectedDeparture = today.plusDays(1),
      )
      val pastUpcomingPlacement = outcome(
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
        requestForPlacementExpectedArrival = today.plusDays(55),
        placementStatus = null,
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(arrived, laterUpcomingPlacement, noPlacement, earliestUpcomingPlacement, pastUpcomingPlacement)

      val upcomingRequestForPlacementSlot = slot<Cas1ExternalRequestForPlacementOutcomeDto?>()
      val requestForPlacementHistorySlot = slot<List<Cas1ExternalRequestForPlacementOutcomeDto>>()
      every {
        transformer.transformToApplicationV2(
          application,
          captureNullable(upcomingRequestForPlacementSlot),
          capture(requestForPlacementHistorySlot),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)

      assertThat(upcomingRequestForPlacementSlot.captured).isEqualTo(earliestUpcomingPlacement.toDto())
      assertThat(requestForPlacementHistorySlot.captured).isEqualTo(listOf(pastUpcomingPlacement.toDto(), arrived.toDto()))
    }

    @Test
    fun `upcoming arrival date is derived from the placement pending date, followed by request for placement date`() {
      val rfpWithRequestedDateIn2DaysButPlacementIn10Days = outcome(
        statusLastUpdated = today.minusDays(5),
        requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
        requestForPlacementExpectedArrival = today.plusDays(2),
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today.plusDays(10),
        placementExpectedDeparture = today.plusDays(20),
      )

      val rfpWithPlacementIn5Days = outcome(
        statusLastUpdated = today.minusDays(5),
        requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
        requestForPlacementExpectedArrival = today.plusDays(5),
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        placementExpectedArrival = today.plusDays(5),
        placementExpectedDeparture = today.plusDays(8),
      )

      every { repository.findByCrn(crn) } returns listOf(application)
      every { outcomeService.getOutcomes(application.id) } returns listOf(rfpWithPlacementIn5Days, rfpWithRequestedDateIn2DaysButPlacementIn10Days)

      val upcomingRequestForPlacementSlot = slot<Cas1ExternalRequestForPlacementOutcomeDto?>()
      val requestForPlacementHistorySlot = slot<List<Cas1ExternalRequestForPlacementOutcomeDto>>()
      every {
        transformer.transformToApplicationV2(
          application,
          captureNullable(upcomingRequestForPlacementSlot),
          capture(requestForPlacementHistorySlot),
        )
      } returns transformed

      assertThat(service.getSuitableApplicationByCrn(crn)).isSameAs(transformed)

      assertThat(upcomingRequestForPlacementSlot.captured).isEqualTo(rfpWithPlacementIn5Days.toDto())
      assertThat(requestForPlacementHistorySlot.captured).isEmpty()
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
    requestForPlacementExpectedArrival: LocalDate? = null,
    placementStatus: Cas1SpaceBookingStatus? = null,
    placementExpectedArrival: LocalDate? = null,
    placementExpectedDeparture: LocalDate? = null,
  ) = RequestForPlacementOutcome(
    requestForPlacement = Cas1ExternalRequestForPlacementDtoV2Factory()
      .withStatus(requestForPlacementStatus)
      .withExpectedArrivalDate(requestForPlacementExpectedArrival)
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
