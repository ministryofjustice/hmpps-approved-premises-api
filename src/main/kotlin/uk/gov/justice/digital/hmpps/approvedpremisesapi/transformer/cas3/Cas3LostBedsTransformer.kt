package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedsEntity

@Component
class Cas3LostBedsTransformer(
  private val cas3LostBedReasonTransformer: Cas3LostBedReasonTransformer,
  private val cas3LostBedCancellationTransformer: Cas3LostBedCancellationTransformer,
) {
  fun transformJpaToApi(jpa: Cas3LostBedsEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    reason = cas3LostBedReasonTransformer.transformJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    status = determineStatus(jpa),
    cancellation = jpa.cancellation?.let { cas3LostBedCancellationTransformer.transformJpaToApi(it) },
    bedId = jpa.bed.id,
    bedName = jpa.bed.name,
    roomName = jpa.bed.room.name,
  )

  private fun determineStatus(jpa: Cas3LostBedsEntity) = when {
    jpa.cancellation != null -> LostBedStatus.cancelled
    else -> LostBedStatus.active
  }
}
