package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestBookingSummaryTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestBookingSummaryTransformerTest {
  private val transformer = PlacementRequestBookingSummaryTransformer()

  @Nested
  inner class SpaceBookingTransform {

    @Test
    fun success() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withId(UUID.randomUUID())
        .withPremises(premises)
        .withCanonicalArrivalDate(LocalDate.now().minusDays(10))
        .withCanonicalDepartureDate(LocalDate.now().plusDays(5))
        .withCreatedAt(OffsetDateTime.now())
        .withCriteria(
          mutableListOf(
            CharacteristicEntityFactory()
              .withPropertyName("hasEnSuite")
              .produce(),
            CharacteristicEntityFactory()
              .withPropertyName("isCatered")
              .produce(),
            CharacteristicEntityFactory()
              .withPropertyName("unmappable")
              .produce(),
          ),
        )
        .produce()

      val result = transformer.transformJpaToApi(spaceBooking)

      assertThat(result.id).isEqualTo(spaceBooking.id)
      assertThat(result.premisesId).isEqualTo(spaceBooking.premises.id)
      assertThat(result.premisesName).isEqualTo(spaceBooking.premises.name)
      assertThat(result.arrivalDate).isEqualTo(spaceBooking.canonicalArrivalDate)
      assertThat(result.departureDate).isEqualTo(spaceBooking.canonicalDepartureDate)
      assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
      assertThat(result.type).isEqualTo(PlacementRequestBookingSummary.Type.space)
      assertThat(result.characteristics).containsExactlyInAnyOrder(
        Cas1SpaceCharacteristic.hasEnSuite,
        Cas1SpaceCharacteristic.isCatered,
      )
    }
  }
}
