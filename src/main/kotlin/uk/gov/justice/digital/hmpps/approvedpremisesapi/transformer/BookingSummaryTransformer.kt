package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity

@Component
class BookingSummaryTransformer {
  fun transformJpaToApi(jpa: BookingEntity): BookingSummary = BookingSummary(
    id = jpa.id,
    premisesId = jpa.premises.id,
    premisesName = jpa.premises.name,
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    createdAt = jpa.createdAt.toInstant(),
    type = BookingSummary.Type.LEGACY,
  )
}
