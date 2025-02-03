package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingSummaryTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingSummaryTransformerTest {

  private val transformer = Cas1SpaceBookingSummaryTransformer()

  val premises = ApprovedPremisesEntityFactory()
    .withDefaults()
    .produce()

  val spaceBooking = Cas1SpaceBookingEntityFactory()
    .withId(UUID.randomUUID())
    .withPremises(premises)
    .withCanonicalArrivalDate(LocalDate.now().minusDays(10))
    .withCanonicalDepartureDate(LocalDate.now().plusDays(5))
    .withCreatedAt(OffsetDateTime.now())
    .produce()

  @Test
  fun success() {
    val result = transformer.transformJpaToApi(spaceBooking)

    assertThat(result.id).isEqualTo(spaceBooking.id)
    assertThat(result.premisesId).isEqualTo(spaceBooking.premises.id)
    assertThat(result.premisesName).isEqualTo(spaceBooking.premises.name)
    assertThat(result.arrivalDate).isEqualTo(spaceBooking.canonicalArrivalDate)
    assertThat(result.departureDate).isEqualTo(spaceBooking.canonicalDepartureDate)
    assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
    assertThat(result.type).isEqualTo(BookingSummary.Type.space)
  }
}
