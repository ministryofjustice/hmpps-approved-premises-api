package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3TurnaroundTransformer
import java.time.OffsetDateTime
import java.util.UUID

class Cas3TurnaroundTransformerTest {
  private val cas3TurnaroundTransformer = Cas3TurnaroundTransformer()

  @Test
  fun `transformJpaToApi transforms the Cas3TurnaroundEntity into a Turnaround`() {
    val booking = BookingEntityFactory()
      .withPremises(
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce(),
      )
      .withServiceName(ServiceName.temporaryAccommodation)
      .produce()

    val turnaroundId = UUID.randomUUID()
    val cas3TurnaroundEntity = Cas3TurnaroundEntity(
      id = turnaroundId,
      createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z"),
      booking = booking,
      workingDayCount = 5,
    )

    val result = cas3TurnaroundTransformer.transformJpaToApi(cas3TurnaroundEntity)

    assertThat(result).isEqualTo(
      Turnaround(
        id = turnaroundId,
        bookingId = booking.id,
        workingDays = 5,
        createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
      ),
    )
  }
}
