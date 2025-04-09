package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ConfirmationTransformer
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ConfirmationTransformerTest {
  private val cas3ConfirmationTransformer = Cas3ConfirmationTransformer()

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

    val confirmationId = UUID.randomUUID()
    val cas3ConfirmationEntity = Cas3ConfirmationEntity(
      id = confirmationId,
      dateTime = OffsetDateTime.parse("2025-04-08T00:00:00Z"),
      notes = "Test notes",
      createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z"),
      booking = booking,
    )

    val result = cas3ConfirmationTransformer.transformJpaToApi(cas3ConfirmationEntity)

    assertThat(result).isEqualTo(
      Confirmation(
        id = confirmationId,
        bookingId = booking.id,
        dateTime = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
        notes = "Test notes",
        createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
      ),
    )
  }
}
