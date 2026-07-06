package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.v2.Cas3v2TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3TurnaroundTransformer
import java.time.OffsetDateTime
import java.util.UUID

class Cas3TurnaroundTransformerTest {
  private val cas3TurnaroundTransformer = Cas3TurnaroundTransformer()

  @Test
  fun `transformJpaToApi transforms the Cas3v2TurnaroundEntity into a Cas3Turnaround`() {
    val booking = Cas3BookingEntityFactory().withDefaults().produce()

    val turnaroundId = UUID.randomUUID()
    val cas3TurnaroundEntity = Cas3v2TurnaroundEntityFactory()
      .withId(turnaroundId)
      .withCreatedAt(OffsetDateTime.parse("2025-04-08T00:00:00Z"))
      .withBooking(booking)
      .withWorkingDayCount(5)
      .produce()

    val result = cas3TurnaroundTransformer.transformJpaToApi(cas3TurnaroundEntity)

    assertThat(result).isEqualTo(
      Cas3Turnaround(
        id = turnaroundId,
        bookingId = booking.id,
        workingDays = 5,
        createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
      ),
    )
  }
}
