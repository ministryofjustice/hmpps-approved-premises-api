package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity

@Component
class BookingNotMadeTransformer {
  fun transformJpaToApi(jpa: BookingNotMadeEntity) = BookingNotMade(
    id = jpa.id,
    placementRequestId = jpa.placementRequest.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
