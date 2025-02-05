package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer.Companion.toCas1SpaceCharacteristics

@Component
class PlacementRequestBookingSummaryTransformer {

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
    characteristics = jpa.criteria.toCas1SpaceCharacteristics(),
  )
}
