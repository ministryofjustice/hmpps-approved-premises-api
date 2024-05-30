package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity

@Component
class Cas1OutOfServiceBedTransformer(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedCancellationTransformer: Cas1OutOfServiceBedCancellationTransformer,
) {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedEntity) = Cas1OutOfServiceBed(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    bedId = jpa.bed.id,
    bedName = jpa.bed.name,
    roomName = jpa.bed.room.name,
    reason = cas1OutOfServiceBedReasonTransformer.transformJpaToApi(jpa.reason),
    status = jpa.deriveStatus(),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    cancellation = jpa.cancellation?.let { cas1OutOfServiceBedCancellationTransformer.transformJpaToApi(it) },
  )

  private fun Cas1OutOfServiceBedEntity.deriveStatus() = when (this.cancellation) {
    null -> Cas1OutOfServiceBedStatus.active
    else -> Cas1OutOfServiceBedStatus.cancelled
  }
}
