package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer

@Component
class Cas3CancellationTransformer(private val cancellationReasonTransformer: CancellationReasonTransformer) {
  fun transformJpaToApi(jpa: Cas3CancellationEntity?) = jpa?.let {
    Cancellation(
      id = jpa.id,
      bookingId = jpa.booking.id,
      date = jpa.date,
      reason = cancellationReasonTransformer.transformJpaToApi(jpa.reason),
      notes = jpa.notes,
      createdAt = jpa.createdAt.toInstant(),
      premisesName = jpa.booking.premises.name,
      otherReason = jpa.otherReason,
    )
  }
}
