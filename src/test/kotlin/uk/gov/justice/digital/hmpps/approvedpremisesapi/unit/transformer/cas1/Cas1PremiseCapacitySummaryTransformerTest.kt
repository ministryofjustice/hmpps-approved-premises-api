package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1PremiseCapacitySummaryTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas1PremiseCapacitySummaryTransformer

  @Test
  fun toCas1PremiseCapacitySummary() {
    val premise = ApprovedPremisesEntityFactory().withDefaults().produce()

    val inputCapacitySummary = PremiseCapacity(
      premises = premise,
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
              availableBedCount = 10,
              bookingCount = 4,
            ),
            PremiseCharacteristicAvailability(
              characteristicPropertyName = "isWheelchairDesignated",
              availableBedCount = 20,
              bookingCount = 8,
            ),
          ),
        ),

      ),
    )

    val result = transformer.toCas1PremiseCapacitySummary(
      premiseCapacity = inputCapacitySummary,
    )

    assertThat(result.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
    assertThat(result.endDate).isEqualTo(LocalDate.of(2021, 3, 4))

    assertThat(result.capacity).hasSize(1)

    val capacityForDay = result.capacity[0]
    assertThat(capacityForDay.date).isEqualTo(LocalDate.of(2020, 1, 3))
    assertThat(capacityForDay.totalBedCount).isEqualTo(5)
    assertThat(capacityForDay.availableBedCount).isEqualTo(3)
    assertThat(capacityForDay.bookingCount).isEqualTo(4)

    assertThat(capacityForDay.characteristicAvailability).hasSize(2)

    val characteristicAvailability1 = capacityForDay.characteristicAvailability[0]
    assertThat(characteristicAvailability1.characteristic).isEqualTo(Cas1SpaceBookingCharacteristic.IS_SINGLE)
    assertThat(characteristicAvailability1.availableBedsCount).isEqualTo(10)
    assertThat(characteristicAvailability1.bookingsCount).isEqualTo(4)

    val characteristicAvailability2 = capacityForDay.characteristicAvailability[1]
    assertThat(characteristicAvailability2.characteristic).isEqualTo(Cas1SpaceBookingCharacteristic.IS_WHEELCHAIR_DESIGNATED)
    assertThat(characteristicAvailability2.availableBedsCount).isEqualTo(20)
    assertThat(characteristicAvailability2.bookingsCount).isEqualTo(8)
  }
}
