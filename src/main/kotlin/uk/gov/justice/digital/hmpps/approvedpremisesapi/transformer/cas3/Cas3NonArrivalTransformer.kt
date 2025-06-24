package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer

@Component
class Cas3NonArrivalTransformer(private val nonArrivalReasonTransformer: NonArrivalReasonTransformer) {
  fun transformJpaToApi(jpa: Cas3NonArrivalEntity?) = jpa?.let {
    Cas3NonArrival(
      id = jpa.id,
      bookingId = jpa.booking.id,
      date = jpa.date,
      reason = nonArrivalReasonTransformer.transformJpaToApi(jpa.reason),
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
