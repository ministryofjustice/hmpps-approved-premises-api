package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesDayTransformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesDayTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas1PremisesDayTransformer

  @Test
  fun toCas1PremisesDaySummary() {
    val currentSearchDay = LocalDate.now()

    val capacity =
      Cas1PremiseCapacityForDay(
        date = currentSearchDay,
        totalBedCount = 5,
        availableBedCount = 3,
        bookingCount = 4,
        characteristicAvailability = listOf(
          Cas1PremiseCharacteristicAvailability(Cas1SpaceBookingCharacteristic.IS_SINGLE, 10, 4),
          Cas1PremiseCharacteristicAvailability(Cas1SpaceBookingCharacteristic.IS_WHEELCHAIR_DESIGNATED, 20, 8),
        ),
      )

    val spaceBookings = listOf(
      Cas1SpaceBookingDaySummary(
        id = UUID.randomUUID(),
        person = RestrictedPersonSummary(
          crn = "crn",
          personType = PersonSummaryDiscriminator.restrictedPersonSummary,
        ),
        canonicalArrivalDate = currentSearchDay.minusDays(1),
        canonicalDepartureDate = currentSearchDay.plusDays(1),
        tier = "Tier 1",
        releaseType = "rotl",
        essentialCharacteristics = listOf(Cas1SpaceBookingCharacteristic.IS_SINGLE, Cas1SpaceBookingCharacteristic.HAS_EN_SUITE),
      ),
    )

    val outOfServiceBeds = listOf(
      Cas1OutOfServiceBedSummary(
        id = UUID.randomUUID(),
        startDate = LocalDate.now().minusDays(5),
        endDate = LocalDate.now().plusDays(5),
        reason = Cas1OutOfServiceBedReason(UUID.randomUUID(), "reason", true),
        characteristics = listOf(Cas1SpaceCharacteristic.isSingle),
      ),
    )

    val result = transformer.toCas1PremisesDaySummary(
      currentSearchDay,
      capacity,
      spaceBookings,
      outOfServiceBeds,
    )

    assertThat(result.forDate).isEqualTo(currentSearchDay)
    assertThat(result.previousDate).isEqualTo(currentSearchDay.minusDays(1))
    assertThat(result.nextDate).isEqualTo(currentSearchDay.plusDays(1))
    assertThat(result.capacity).isEqualTo(capacity)
    assertThat(result.spaceBookings).isEqualTo(spaceBookings)
    assertThat(result.outOfServiceBeds).isEqualTo(outOfServiceBeds)
  }
}
