package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity

@Component
class CancellationTransformer(private val cancellationReasonTransformer: CancellationReasonTransformer) {
  fun transformJpaToApi(jpa: CancellationEntity?) = jpa?.let {
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
