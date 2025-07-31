package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NonArrival
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
