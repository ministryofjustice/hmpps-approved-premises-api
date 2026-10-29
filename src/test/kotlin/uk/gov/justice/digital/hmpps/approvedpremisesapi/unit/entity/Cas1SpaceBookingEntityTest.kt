package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import java.time.Instant
import java.time.LocalDate

class Cas1SpaceBookingEntityTest {

  @Nested
  inner class GetSpaceBookingStatus {

    private fun booking() = Cas1SpaceBookingEntityFactory()
      .withExpectedArrivalDate(LocalDate.now())
      .withActualArrivalDate(null)
      .withActualDepartureDate(null)
      .withCancellationOccurredAt(null)
      .withNonArrivalConfirmedAt(null)

    @Test
    fun `Returns upcoming with expected arrival date when no arrival, departure, non arrival or cancellation`() {
      val expectedArrivalDate = LocalDate.of(2025, 1, 10)
      val result = booking().withExpectedArrivalDate(expectedArrivalDate).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.UPCOMING)
      assertThat(result.occurredAt).isEqualTo(expectedArrivalDate)
    }

    @Test
    fun `Returns arrived with actual arrival date when arrived but not departed`() {
      val actualArrival = LocalDate.of(2025, 1, 11)

      val result = booking().withActualArrivalDate(actualArrival).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.ARRIVED)
      assertThat(result.occurredAt).isEqualTo(actualArrival)
    }

    @Test
    fun `Returns departed with actual departure date when arrived and departed`() {
      val actualDeparture = LocalDate.of(2025, 2, 1)

      val result = booking()
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(actualDeparture)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.DEPARTED)
      assertThat(result.occurredAt).isEqualTo(actualDeparture)
    }

    @Test
    fun `Returns not arrived with expected arrival date when non arrival confirmed`() {
      val expectedArrivalDate = LocalDate.of(2025, 1, 10)
      val result = booking().withExpectedArrivalDate(expectedArrivalDate).withNonArrivalConfirmedAt(Instant.now()).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(expectedArrivalDate)
    }

    @Test
    fun `Returns cancelled with cancellation occurred date when cancelled`() {
      val cancelledOn = LocalDate.of(2025, 1, 5)

      val result = booking().withCancellationOccurredAt(cancelledOn).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancelledOn)
    }

    @Test
    fun `Cancelled takes precedence over non arrival, departure and arrival`() {
      val cancelledOn = LocalDate.of(2025, 1, 5)

      val result = booking()
        .withCancellationOccurredAt(cancelledOn)
        .withNonArrivalConfirmedAt(Instant.now())
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(LocalDate.of(2025, 2, 1))
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancelledOn)
    }

    @Test
    fun `Non arrival takes precedence over departure and arrival`() {
      val expectedArrivalDate = LocalDate.of(2025, 1, 10)
      val result = booking()
        .withExpectedArrivalDate(expectedArrivalDate)
        .withNonArrivalConfirmedAt(Instant.now())
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(LocalDate.of(2025, 2, 1))
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(expectedArrivalDate)
    }
  }
}
