package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacitySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate

class Cas1PremiseCapacitySummaryTransformerTest {

  @Test
  fun toCas1PremiseCapacitySummary() {
    val premise = ApprovedPremisesEntityFactory().withDefaults().produce()

    val input = PremiseCapacitySummary(
      premise = premise,
      range = DateRange(
        LocalDate.of(2020, 1, 2),
        LocalDate.of(2021, 3, 4),
      ),
      byDay = listOf(
        PremiseCapacityForDay(
          day = LocalDate.of(2020, 1, 3),
          totalBedCount = 5,
          availableBedCount = 3,
          bookingCount = 4,
          characteristicAvailability = listOf(
            PremiseCharacteristicAvailability(
              characteristicPropertyName = "isSingle",
              capacity = 10,
              available = 4,
            ),
            PremiseCharacteristicAvailability(
              characteristicPropertyName = "isWheelchairAccessible",
              capacity = 20,
              available = 8,
            ),
          ),
        ),

      ),
    )

    val result = Cas1PremiseCapacitySummaryTransformer().toCas1PremiseCapacitySummary(input)

    assertThat(result.premisesId).isEqualTo(premise.id)
    assertThat(result.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
    assertThat(result.endDate).isEqualTo(LocalDate.of(2021, 3, 4))

    assertThat(result.capacity).hasSize(1)

    val capacityForDay = result.capacity[0]
    assertThat(capacityForDay.totalBedCount).isEqualTo(5)
    assertThat(capacityForDay.availableBedCount).isEqualTo(3)
    assertThat(capacityForDay.bookingCount).isEqualTo(4)

    assertThat(capacityForDay.characteristicAvailability).hasSize(2)

    val characteristicAvailability1 = capacityForDay.characteristicAvailability[0]
    assertThat(characteristicAvailability1.characteristic).isEqualTo(Cas1SpaceCharacteristic.isSingle)
    assertThat(characteristicAvailability1.capacity).isEqualTo(10)
    assertThat(characteristicAvailability1.available).isEqualTo(4)

    val characteristicAvailability2 = capacityForDay.characteristicAvailability[1]
    assertThat(characteristicAvailability2.characteristic).isEqualTo(Cas1SpaceCharacteristic.isWheelchairAccessible)
    assertThat(characteristicAvailability2.capacity).isEqualTo(20)
    assertThat(characteristicAvailability2.available).isEqualTo(8)
  }
}
