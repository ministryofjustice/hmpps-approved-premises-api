package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.CandidatePremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1NationalOccupancyTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1NationalOccupancyTransformerTest {

  @MockK
  lateinit var searchResultsTransformer: Cas1SpaceSearchResultsTransformer

  @InjectMockKs
  lateinit var transformer: Cas1NationalOccupancyTransformer

  companion object {
    val PREMISES_ID = UUID.randomUUID()
  }

  @Nested
  inner class ToCapacitySummary {

    @Test
    fun `no room characteristics defined, return overall capacity numbers`() {
      val startDate = LocalDate.of(2021, 1, 1)
      val endDate = LocalDate.of(2021, 1, 2)

      val candidatePremises = CandidatePremisesFactory().withId(PREMISES_ID).produce()
      val cas1PremisesSearchResultSummary = mockk<Cas1PremisesSearchResultSummary>()
      every { searchResultsTransformer.toPremisesSearchResultSummary(candidatePremises) } returns cas1PremisesSearchResultSummary

      val result = transformer.toCapacitySummary(
        requestedRoomCharacteristics = emptySet(),
        premisesSummaries = listOf(candidatePremises),
        capacities = Cas1PremisesService.Cas1PremisesCapacities(
          startDate = startDate,
          endDate = endDate,
          results = listOf(
            SpacePlanningService.PremiseCapacity(
              premisesId = PREMISES_ID,
              range = DateRange(startDate, endDate),
              byDay = listOf(
                PremiseCapacityForDay(
                  day = startDate,
                  totalBedCount = 100,
                  availableBedCount = 90,
                  bookingCount = 75,
                  characteristicAvailability = emptyList(),
                ),
                PremiseCapacityForDay(
                  day = endDate,
                  totalBedCount = 50,
                  availableBedCount = 49,
                  bookingCount = 50,
                  characteristicAvailability = emptyList(),
                ),
              ),
            ),
          ),
        ),
      )

      assertThat(result.startDate).isEqualTo(startDate)
      assertThat(result.endDate).isEqualTo(endDate)
      assertThat(result.premises).hasSize(1)

      val premisesOccupancy = result.premises[0]
      assertThat(premisesOccupancy.summary).isEqualTo(cas1PremisesSearchResultSummary)
      assertThat(premisesOccupancy.capacity).hasSize(2)

      val capacityDay1 = premisesOccupancy.capacity[0]
      assertThat(capacityDay1.date).isEqualTo(startDate)
      assertThat(capacityDay1.forRoomCharacteristic).isNull()
      assertThat(capacityDay1.inServiceBedCount).isEqualTo(90)
      assertThat(capacityDay1.vacantBedCount).isEqualTo(15)

      val capacityDay2 = premisesOccupancy.capacity[1]
      assertThat(capacityDay2.date).isEqualTo(endDate)
      assertThat(capacityDay2.forRoomCharacteristic).isNull()
      assertThat(capacityDay2.inServiceBedCount).isEqualTo(49)
      assertThat(capacityDay2.vacantBedCount).isEqualTo(-1)
    }
  }

  @Test
  fun `room characteristics defined, provide lowest occupancy`() {
    val startDate = LocalDate.of(2021, 1, 1)
    val endDate = LocalDate.of(2021, 1, 2)

    val candidatePremises = CandidatePremisesFactory().withId(PREMISES_ID).produce()
    val cas1PremisesSearchResultSummary = mockk<Cas1PremisesSearchResultSummary>()
    every { searchResultsTransformer.toPremisesSearchResultSummary(candidatePremises) } returns cas1PremisesSearchResultSummary

    val result = transformer.toCapacitySummary(
      requestedRoomCharacteristics = setOf(
        Cas1SpaceCharacteristic.hasEnSuite,
        Cas1SpaceCharacteristic.isSingle,
      ),
      premisesSummaries = listOf(candidatePremises),
      capacities = Cas1PremisesService.Cas1PremisesCapacities(
        startDate = startDate,
        endDate = endDate,
        results = listOf(
          SpacePlanningService.PremiseCapacity(
            premisesId = PREMISES_ID,
            range = DateRange(startDate, endDate),
            byDay = listOf(
              PremiseCapacityForDay(
                day = startDate,
                totalBedCount = 100,
                availableBedCount = 90,
                bookingCount = 75,
                characteristicAvailability = listOf(
                  // this has lowest capacity, but is not a characteritic of interest so ignored
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
                    availableBedCount = 0,
                    bookingCount = 0,
                  ),
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_ENSUITE,
                    availableBedCount = 50,
                    bookingCount = 25,
                  ),
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_SINGLE_ROOM,
                    availableBedCount = 30,
                    bookingCount = 3,
                  ),
                ),
              ),
              PremiseCapacityForDay(
                day = endDate,
                totalBedCount = 50,
                availableBedCount = 49,
                bookingCount = 1,
                characteristicAvailability = listOf(
                  // this has lowest capacity, but is not a characteristic of interest so ignored
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
                    availableBedCount = 0,
                    bookingCount = 0,
                  ),
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_ENSUITE,
                    availableBedCount = 1,
                    bookingCount = 1,
                  ),
                  PremiseCharacteristicAvailability(
                    characteristicPropertyName = CAS1_PROPERTY_NAME_SINGLE_ROOM,
                    availableBedCount = 30,
                    bookingCount = 3,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(result.startDate).isEqualTo(startDate)
    assertThat(result.endDate).isEqualTo(endDate)
    assertThat(result.premises).hasSize(1)

    val premisesOccupancy = result.premises[0]
    assertThat(premisesOccupancy.summary).isEqualTo(cas1PremisesSearchResultSummary)
    assertThat(premisesOccupancy.capacity).hasSize(2)

    val capacityDay1 = premisesOccupancy.capacity[0]
    assertThat(capacityDay1.date).isEqualTo(startDate)
    assertThat(capacityDay1.forRoomCharacteristic).isNull()
    assertThat(capacityDay1.inServiceBedCount).isEqualTo(90)
    assertThat(capacityDay1.vacantBedCount).isEqualTo(15)

    val capacityDay2 = premisesOccupancy.capacity[1]
    assertThat(capacityDay2.date).isEqualTo(endDate)
    assertThat(capacityDay2.forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.hasEnSuite)
    assertThat(capacityDay2.inServiceBedCount).isEqualTo(1)
    assertThat(capacityDay2.vacantBedCount).isEqualTo(0)
  }
}
