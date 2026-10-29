package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

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
    fun `Returns upcoming with creation date when no arrival, departure, non arrival or cancellation`() {
      val creationDate = OffsetDateTime.now().minusDays(5)
      val result = booking().withCreatedAt(creationDate).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.UPCOMING)
      assertThat(result.occurredAt).isEqualTo(creationDate.toLocalDateTime())
    }

    @Test
    fun `Returns arrived with actual arrival date and time when arrived but not departed`() {
      val actualArrival = LocalDate.of(2025, 1, 11)
      val actualArrivalTime = LocalTime.parse("12:14")

      val result = booking()
        .withActualArrivalDate(actualArrival)
        .withActualArrivalTime(actualArrivalTime)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.ARRIVED)
      assertThat(result.occurredAt).isEqualTo(LocalDateTime.parse("2025-01-11T12:14"))
    }

    @Test
    fun `Returns arrived with actual arrival date with no time when arrived but not departed`() {
      val actualArrival = LocalDate.of(2025, 1, 11)

      val result = booking()
        .withActualArrivalDate(actualArrival)
        .withActualArrivalTime(null)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.ARRIVED)
      assertThat(result.occurredAt).isEqualTo(LocalDateTime.parse("2025-01-11T00:00"))
    }

    @Test
    fun `Returns departed with actual departure date and time when arrived and departed`() {
      val actualDeparture = LocalDate.of(2025, 2, 1)
      val actualDepartureTime = LocalTime.parse("01:59")

      val result = booking()
        .withActualDepartureDate(actualDeparture)
        .withActualDepartureTime(actualDepartureTime)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.DEPARTED)
      assertThat(result.occurredAt).isEqualTo(LocalDateTime.parse("2025-02-01T01:59"))
    }

    @Test
    fun `Returns departed with actual departure date and no time when arrived and departed`() {
      val actualDeparture = LocalDate.of(2025, 2, 1)

      val result = booking()
        .withActualDepartureDate(actualDeparture)
        .withActualDepartureTime(null)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.DEPARTED)
      assertThat(result.occurredAt).isEqualTo(LocalDateTime.parse("2025-02-01T00:00"))
    }

    @Test
    fun `Returns not arrived with expected arrival date when non arrival confirmed`() {
      val expectedArrivalDate = LocalDate.of(2025, 1, 10)
      val result = booking().withExpectedArrivalDate(expectedArrivalDate).withNonArrivalConfirmedAt(Instant.now()).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(expectedArrivalDate.atStartOfDay())
    }

    @Test
    fun `Returns cancelled with cancellation occurred date when cancelled`() {
      val cancelledOn = LocalDate.of(2025, 1, 5)

      val result = booking().withCancellationOccurredAt(cancelledOn).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancelledOn.atStartOfDay())
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

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancelledOn.atStartOfDay())
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

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(expectedArrivalDate.atStartOfDay())
    }
  }
}
