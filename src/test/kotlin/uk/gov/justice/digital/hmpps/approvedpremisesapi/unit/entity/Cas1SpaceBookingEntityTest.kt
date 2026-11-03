package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.Instant
import java.time.LocalDate
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
    fun `Returns upcoming with creation date date when no arrival, departure, non arrival or cancellation`() {
      val creationDateTime = OffsetDateTime.now().minusDays(5)
      val result = booking().withCreatedAt(creationDateTime).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.UPCOMING)
      assertThat(result.occurredAt).isEqualTo(creationDateTime.toLocalDateTime())
    }

    @Test
    fun `Returns arrived with actual arrival date when arrived but not departed`() {
      val actualArrival = LocalDate.of(2025, 1, 11)
      val actualArrivalTime = LocalTime.of(12, 52)

      val result = booking()
        .withActualArrivalDate(actualArrival)
        .withActualArrivalTime(actualArrivalTime)
        .produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.ARRIVED)
      assertThat(result.occurredAt).isEqualTo(actualArrival.atTime(actualArrivalTime))
    }

    @Test
    fun `Returns arrived with actual arrival date when arrived but not departed, no time set`() {
      val actualArrival = LocalDate.of(2025, 1, 11)

      val result = booking()
        .withActualArrivalDate(actualArrival)
        .withActualArrivalTime(null)
        .produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.ARRIVED)
      assertThat(result.occurredAt).isEqualTo(actualArrival.atStartOfDay())
    }

    @Test
    fun `Returns departed with actual departure date when arrived and departed`() {
      val actualDeparture = LocalDate.of(2025, 2, 1)
      val actualDepartureTime = LocalTime.of(14, 15)

      val result = booking()
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(actualDeparture)
        .withActualDepartureTime(actualDepartureTime)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.DEPARTED)
      assertThat(result.occurredAt).isEqualTo(actualDeparture.atTime(actualDepartureTime))
    }

    @Test
    fun `Returns departed with actual departure date when arrived and departed, no time set`() {
      val actualDeparture = LocalDate.of(2025, 2, 1)

      val result = booking()
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(actualDeparture)
        .withActualDepartureTime(null)
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.DEPARTED)
      assertThat(result.occurredAt).isEqualTo(actualDeparture.atStartOfDay())
    }

    @Test
    fun `Returns not arrived with expected arrival date when non arrival confirmed`() {
      val nonArrivalRecordedAt = Instant.now().minusDays(2)
      val result = booking().withNonArrivalConfirmedAt(nonArrivalRecordedAt).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(nonArrivalRecordedAt.toLocalDateTime())
    }

    @Test
    fun `Returns cancelled with cancellation occurred date when cancelled`() {
      val cancellationRecordedAt = Instant.now().minusDays(3)

      val result = booking()
        .withCancellationOccurredAt(LocalDate.now())
        .withCancellationRecordedAt(cancellationRecordedAt).produce().getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancellationRecordedAt.toLocalDateTime())
    }

    @Test
    fun `Cancelled takes precedence over non arrival, departure and arrival`() {
      val cancellationRecordedAt = Instant.now().minusDays(3)

      val result = booking()
        .withCancellationOccurredAt(LocalDate.now())
        .withCancellationRecordedAt(cancellationRecordedAt)
        .withNonArrivalConfirmedAt(Instant.now())
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(LocalDate.of(2025, 2, 1))
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.CANCELLED)
      assertThat(result.occurredAt).isEqualTo(cancellationRecordedAt.toLocalDateTime())
    }

    @Test
    fun `Non arrival takes precedence over departure and arrival`() {
      val nonArrivalRecordedAt = Instant.now().minusDays(2)

      val expectedArrivalDate = LocalDate.of(2025, 1, 10)
      val result = booking()
        .withExpectedArrivalDate(expectedArrivalDate)
        .withNonArrivalConfirmedAt(nonArrivalRecordedAt)
        .withActualArrivalDate(LocalDate.of(2025, 1, 11))
        .withActualDepartureDate(LocalDate.of(2025, 2, 1))
        .produce()
        .getSpaceBookingStatus()

      assertThat(result.status).isEqualTo(Cas1SpaceBookingEntity.Status.NOT_ARRIVED)
      assertThat(result.occurredAt).isEqualTo(nonArrivalRecordedAt.toLocalDateTime())
    }
  }
}
