package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ConfirmationTransformer
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ConfirmationTransformerTest {
  private val cas3ConfirmationTransformer = Cas3ConfirmationTransformer()

  @Test
  fun `transformJpaToApi transforms the Cas3ConfirmationEntity into a Confirmation`() {
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

  @Test
  fun `transformJpaToApi transforms the Cas3v2ConfirmationEntity into a Cas3Confirmation`() {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val booking = Cas3BookingEntityFactory()
      .withPremises(premises)
      .withBedspace(bedspace)
      .withServiceName(ServiceName.temporaryAccommodation)
      .produce()

    val confirmationId = UUID.randomUUID()
    val cas3ConfirmationEntity = Cas3v2ConfirmationEntity(
      id = confirmationId,
      dateTime = OffsetDateTime.parse("2025-04-08T00:00:00Z"),
      notes = "Test notes",
      createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z"),
      booking = booking,
    )

    val result = cas3ConfirmationTransformer.transformJpaToApi(cas3ConfirmationEntity)

    assertThat(result).isEqualTo(
      Cas3Confirmation(
        id = confirmationId,
        bookingId = booking.id,
        dateTime = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
        notes = "Test notes",
        createdAt = OffsetDateTime.parse("2025-04-08T00:00:00Z").toInstant(),
      ),
    )
  }
}
