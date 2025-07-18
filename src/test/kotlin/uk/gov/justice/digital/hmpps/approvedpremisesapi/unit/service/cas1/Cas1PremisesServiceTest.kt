package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OverbookingRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesServiceTest {

  @MockK
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @MockK
  lateinit var premisesService: PremisesService

  @MockK
  lateinit var outOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  lateinit var spacePlanningService: SpacePlanningService

  @MockK
  lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  lateinit var bedRepository: BedRepository

  @MockK
  lateinit var cas1BedsRepository: Cas1BedsRepository

  @MockK
  lateinit var featureFlagService: FeatureFlagService

  @MockK
  lateinit var cas1OccupancyReportRepository: Cas1OccupancyReportRepository

  @MockK
  lateinit var cas1PremisesLocalRestrictionRepository: Cas1PremisesLocalRestrictionRepository

  @MockK
  lateinit var clock: Clock

  @InjectMockKs
  lateinit var service: Cas1PremisesService

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.fromString("98fe68f2-a58e-4e55-9d0e-f40856ccde24")
  }

  @Nested
  inner class GetPremisesInfo {

    @Test
    fun `premises not found return error`() {
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremisesInfo(PREMISES_ID)

      assertThatCasResult(result).isNotFound("premises", PREMISES_ID)
    }

    @Test
    fun success() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(12)),
        byDay = emptyList(),
      )

      every { featureFlagService.getBooleanFlag(eq("cas1-disable-overbooking-summary")) } returns false
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary
      every { spaceBookingRepository.countActiveSpaceBookings(PREMISES_ID) } returns 4

      val result = service.getPremisesInfo(PREMISES_ID)

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
        assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
        assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
        assertThat(premisesSummaryInfo.availableBeds).isEqualTo(48)
        assertThat(premisesSummaryInfo.overbookingSummary).isEmpty()
      }
    }

    @Test
    fun shouldCalculateCorrectOverBookingSummary() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(12)),
        byDay = listOf(PremiseCapacityForDay(LocalDate.of(2024, 11, 12), 5, 2, 7, emptyList())),
      )

      every { featureFlagService.getBooleanFlag(eq("cas1-disable-overbooking-summary")) } returns false
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary
      every { spaceBookingRepository.countActiveSpaceBookings(PREMISES_ID) } returns 5

      val result = service.getPremisesInfo(PREMISES_ID)

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
        assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
        assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
        assertThat(premisesSummaryInfo.availableBeds).isEqualTo(47)
        assertThat(premisesSummaryInfo.overbookingSummary).isEqualTo(listOf(Cas1OverbookingRange(LocalDate.of(2024, 11, 12), LocalDate.of(2024, 11, 12))))
      }
    }
  }

  @Nested
  inner class GetPremisesInfoOverbooking {

    private lateinit var premises: ApprovedPremisesEntity
    private lateinit var rangeStart: LocalDate
    private lateinit var rangeEnd: LocalDate

    @BeforeEach
    fun setup() {
      premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      rangeStart = LocalDate.of(2024, 7, 1)
      rangeEnd = rangeStart.plusWeeks(12)

      every { featureFlagService.getBooleanFlag(eq("cas1-disable-overbooking-summary")) } returns false
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises
      every { spaceBookingRepository.countActiveSpaceBookings(eq(premises.id)) } returns 100
      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
    }

    @Test
    fun `should handle no overbooking and returns emptyList`() {
      val overbookedDays = listOf(
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 4, 3, 3, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 2), 4, 3, 3, emptyList()),
      )

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it

        assertThat(premisesSummaryInfo.overbookingSummary).isEmpty()
      }
    }

    @Test
    fun `should return correct overbooking ranges for same month`() {
      val overbookedDays = listOf(
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 2), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 3), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(
          LocalDate.of(2024, 7, 4),
          3,
          3,
          1,
          listOf(PremiseCharacteristicAvailability("single room", 0, 5)),
        ),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 5), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 10), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 11), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 12), 3, 1, 4, emptyList()),
      )

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)

        val expectedRanges = listOf(
          Cas1OverbookingRange(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5)),
          Cas1OverbookingRange(LocalDate.of(2024, 7, 10), LocalDate.of(2024, 7, 12)),
        )

        assertEquals(expectedRanges, premisesSummaryInfo.overbookingSummary)
      }
    }

    @Test
    fun `should return empty list for correct overbooking ranges for same month when cas1-disable-overbooking-summary feature flag is switched on`() {
      val overbookedDays = listOf(
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 2), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 3), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(
          LocalDate.of(2024, 7, 4),
          3,
          3,
          1,
          listOf(PremiseCharacteristicAvailability("single room", 0, 5)),
        ),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 5), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 10), 3, 0, 4, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 11), 3, 0, 5, emptyList()),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 12), 3, 1, 4, emptyList()),
      )

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { featureFlagService.getBooleanFlag(eq("cas1-disable-overbooking-summary")) } returns true
      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)

        assertEquals(emptyList<Cas1OverbookingRange>(), premisesSummaryInfo.overbookingSummary)
      }
    }

    @Test
    fun `should handle PremiseCharacteristicAvailability overbooking and returns correct range`() {
      val overbookedDays = listOf(
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 4, 3, 3, emptyList()),
        createPremiseCapacityForDay(
          LocalDate.of(2024, 7, 2),
          4,
          5,
          3,
          listOf(PremiseCharacteristicAvailability("single room", 0, 5)),
        ),
        createPremiseCapacityForDay(
          LocalDate.of(2024, 7, 4),
          3,
          3,
          1,
          listOf(PremiseCharacteristicAvailability("single room", 0, 5)),
        ),
      )

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)

        val expectedRanges = listOf(
          Cas1OverbookingRange(LocalDate.of(2024, 7, 2), LocalDate.of(2024, 7, 2)),
          Cas1OverbookingRange(LocalDate.of(2024, 7, 4), LocalDate.of(2024, 7, 4)),
        )

        assertEquals(expectedRanges, premisesSummaryInfo.overbookingSummary)
      }
    }

    @Test
    fun `should handles 12 week period overbooking and return correct ranges`() {
      val overbookedDays = listOf(
        // First range: June 1 to June 5
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 1), 5, 0, 7),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 2), 5, 0, 8),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 3), 5, 0, 5),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 4), 4, 2, 3),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 5), 4, 0, 6),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 6), 6, 0, 8),
        createPremiseCapacityForDay(LocalDate.of(2024, 6, 7), 5, 0, 7),

        // Second range: July 1 to July 10
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 5, 0, 7),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 2), 4, 0, 5),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 5), 4, 0, 6),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 10), 5, 0, 4),

        // Third range: August 20 to August 25
        createPremiseCapacityForDay(LocalDate.of(2024, 8, 20), 4, 1, 5),
        createPremiseCapacityForDay(LocalDate.of(2024, 8, 21), 5, 0, 7),
        createPremiseCapacityForDay(LocalDate.of(2024, 8, 25), 5, 1, 3),
      )

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)

        val expectedRanges = listOf(
          Cas1OverbookingRange(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 7)),

          Cas1OverbookingRange(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 2)),
          Cas1OverbookingRange(LocalDate.of(2024, 7, 5), LocalDate.of(2024, 7, 5)),
          Cas1OverbookingRange(LocalDate.of(2024, 7, 10), LocalDate.of(2024, 7, 10)),

          Cas1OverbookingRange(LocalDate.of(2024, 8, 20), LocalDate.of(2024, 8, 21)),
          Cas1OverbookingRange(LocalDate.of(2024, 8, 25), LocalDate.of(2024, 8, 25)),

        )

        assertEquals(expectedRanges, premisesSummaryInfo.overbookingSummary)
      }
    }

    @Test
    fun `should handle non-overbooking day within an overbooking period`() {
      val overbookedDays = listOf(
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 1), 5, 0, 7),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 2), 8, 5, 3),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 4), 5, 0, 8),
        createPremiseCapacityForDay(LocalDate.of(2024, 7, 5), 6, 0, 6),
      )
      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(rangeStart, rangeEnd),
        byDay = overbookedDays,
      )

      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesInfo(PREMISES_ID) as CasResult.Success

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)

        val expectedRanges = listOf(
          Cas1OverbookingRange(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1)),
          Cas1OverbookingRange(LocalDate.of(2024, 7, 4), LocalDate.of(2024, 7, 5)),
        )
        assertEquals(expectedRanges, premisesSummaryInfo.overbookingSummary)
      }
    }

    private fun createPremiseCapacityForDay(
      date: LocalDate,
      totalBeds: Int,
      availableBeds: Int,
      bookings: Int,
      characteristics: List<PremiseCharacteristicAvailability> = emptyList(),
    ): PremiseCapacityForDay = PremiseCapacityForDay(
      day = date,
      totalBedCount = totalBeds,
      availableBedCount = availableBeds,
      bookingCount = bookings,
      characteristicAvailability = characteristics,
    )
  }

  @Nested
  inner class GetPremisesCapacities {

    @Test
    fun `at least one premises not found return error`() {
      every { approvedPremisesRepository.findAllById(listOf(PREMISES_ID)) } returns emptyList()

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThatCasResult(result).isGeneralValidationError("Could not resolve all premises IDs. Missing IDs are [98fe68f2-a58e-4e55-9d0e-f40856ccde24]")
    }

    @Test
    fun `start before end date return error`() {
      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(ApprovedPremisesEntityFactory().withDefaults().produce())

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 4),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThatCasResult(result).isGeneralValidationError("Start Date 2020-01-04 should be before End Date 2020-01-03")
    }

    @Test
    fun `success, no premises requested`() {
      val result = service.getPremisesCapacities(
        premisesIds = emptyList(),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
        assertThat(it.endDate).isEqualTo(LocalDate.of(2020, 1, 3))
        assertThat(it.results).isEmpty()
      }
    }

    @Test
    fun `success, single premises`() {
      val premise = ApprovedPremisesEntityFactory().withDefaults().withId(PREMISES_ID).produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(premise)

      val capacityResponse = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(PREMISES_ID),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2020, 1, 3),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
        assertThat(it.endDate).isEqualTo(LocalDate.of(2020, 1, 3))
        assertThat(it.results).isEqualTo(listOf(capacityResponse))
      }
    }

    @Test
    fun `success, multiple premises`() {
      val premises1 = ApprovedPremisesEntityFactory().withDefaults().produce()
      val premises2 = ApprovedPremisesEntityFactory().withDefaults().produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(premises1.id, premises2.id))
      } returns listOf(premises1, premises2)

      val capacityResponse1 = mockk<PremiseCapacity>()
      val capacityResponse2 = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(premises1.id, premises2.id),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 2, 2),
            LocalDate.of(2020, 2, 3),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse1, capacityResponse2)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(premises1.id, premises2.id),
        startDate = LocalDate.of(2020, 2, 2),
        endDate = LocalDate.of(2020, 2, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.results).isEqualTo(listOf(capacityResponse1, capacityResponse2))
      }
    }

    @Test
    fun `success truncate to less than 2 years`() {
      val premise = ApprovedPremisesEntityFactory().withDefaults().withId(PREMISES_ID).produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(premise)

      val capacityResponse = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(PREMISES_ID),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2022, 1, 1),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2023, 1, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.results).isEqualTo(listOf(capacityResponse))
      }
    }
  }

  @Nested
  inner class PremisesLocalRestrictions {

    @Test
    fun `successfully add restriction`() {
      val userID = UUID.randomUUID()
      val description = "description"
      val restrictionCaptor = slot<Cas1PremisesLocalRestrictionEntity>()

      every { cas1PremisesLocalRestrictionRepository.saveAndFlush(capture(restrictionCaptor)) } answers { restrictionCaptor.captured }

      service.addLocalRestriction(
        premisesId = PREMISES_ID,
        createdByUserId = userID,
        description = description,
      )

      verify(exactly = 1) { cas1PremisesLocalRestrictionRepository.saveAndFlush(any()) }
      val savedEntity = restrictionCaptor.captured

      assertEquals(description, savedEntity.description)
      assertEquals(userID, savedEntity.createdByUserId)
      assertEquals(PREMISES_ID, savedEntity.approvedPremisesId)
    }
  }
}
