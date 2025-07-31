package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer

@Component
class Cas3CancellationTransformer(private val cancellationReasonTransformer: CancellationReasonTransformer) {
  fun transformJpaToApi(jpa: Cas3CancellationEntity?) = jpa?.let {
    Cas3Cancellation(
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
