package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class PlacementRequestBookingSummaryTransformer {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun transformJpaToApi(jpa: BookingEntity) = PlacementRequestBookingSummary(
    id = jpa.id,
    premisesId = jpa.premises.id,
    premisesName = jpa.premises.name,
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    createdAt = jpa.createdAt.toInstant(),
    type = PlacementRequestBookingSummary.Type.legacy,
    characteristics = null,
  )

  @SuppressWarnings("SwallowedException")
  fun transformJpaToApi(jpa: Cas1SpaceBookingEntity) = PlacementRequestBookingSummary(
    id = jpa.id,
    premisesId = jpa.premises.id,
    premisesName = jpa.premises.name,
    arrivalDate = jpa.canonicalArrivalDate,
    departureDate = jpa.canonicalDepartureDate,
    createdAt = jpa.createdAt.toInstant(),
    type = PlacementRequestBookingSummary.Type.space,
    characteristics = jpa.criteria.mapNotNull { it.toCas1SpaceCharacteristicOrNull() },
  )

  private fun CharacteristicEntity.toCas1SpaceCharacteristicOrNull() =
    Cas1SpaceCharacteristic.entries.find { it.name == propertyName } ?: run {
      log.warn("Couldn't find a Cas1SpaceCharacteristic enum entry for propertyName $propertyName")
      null
    }
}
