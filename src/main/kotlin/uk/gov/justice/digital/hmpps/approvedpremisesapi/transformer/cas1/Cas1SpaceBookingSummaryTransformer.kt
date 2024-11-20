package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity

@Component
class Cas1SpaceBookingSummaryTransformer {
  fun transformJpaToApi(jpa: Cas1SpaceBookingEntity) = BookingSummary(
    id = jpa.id,
    premisesId = jpa.premises.id,
    premisesName = jpa.premises.name,
    arrivalDate = jpa.canonicalArrivalDate,
    departureDate = jpa.canonicalDepartureDate,
    createdAt = jpa.createdAt.toInstant(),
  )
}
