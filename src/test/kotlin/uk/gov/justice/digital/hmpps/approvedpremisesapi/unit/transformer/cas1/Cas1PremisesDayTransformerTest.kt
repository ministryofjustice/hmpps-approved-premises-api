package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesDayTransformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesDayTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas1PremisesDayTransformer

  @Test
  fun toCas1PremisesDaySummary() {
    val premise = ApprovedPremisesEntityFactory().withDefaults().produce()
    val cas1PremisesSummary = Cas1PremisesSummary(
      id = premise.id,
      name = premise.name,
      apCode = premise.apCode,
      postcode = premise.postcode,
      apArea = ApArea(UUID.randomUUID(), "identifier", "name"),
      bedCount = 5,
      availableBeds = 4,
      outOfServiceBeds = 1,
      supportsSpaceBookings = true,
      overbookingSummary = emptyList(),
      managerDetails = "manager details",
    )

    val currentSearchDay = LocalDate.now()

    val capacity = listOf(
      Cas1PremiseCapacityForDay(
        date = currentSearchDay,
        totalBedCount = 5,
        availableBedCount = 3,
        bookingCount = 4,
        characteristicAvailability = listOf(
          Cas1PremiseCharacteristicAvailability(Cas1SpaceCharacteristic.isSingle, 10, 4),
          Cas1PremiseCharacteristicAvailability(Cas1SpaceCharacteristic.isWheelchairAccessible, 20, 8),
        ),
      ),
    )

    val premiseCapacity = Cas1PremiseCapacity(
      premise = cas1PremisesSummary,
      startDate = currentSearchDay,
      endDate = currentSearchDay,
      capacity = capacity,
    )

    val result = transformer.toCas1PremisesDaySummary(
      currentSearchDay,
      premiseCapacity,
    )

    assertThat(result.forDate).isEqualTo(currentSearchDay)
    assertThat(result.previousDate).isEqualTo(currentSearchDay.minusDays(1))
    assertThat(result.nextDate).isEqualTo(currentSearchDay.plusDays(1))
    assertThat(result.capacity).isEqualTo(capacity[0])
    assertThat(result.spaceBookings).isEmpty()
    assertThat(result.outOfServiceBeds).isEmpty()
  }
}
