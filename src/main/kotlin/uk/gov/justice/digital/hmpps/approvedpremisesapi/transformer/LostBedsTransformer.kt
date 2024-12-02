package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity

@Component
class LostBedsTransformer(
  private val lostBedReasonTransformer: LostBedReasonTransformer,
  private val lostBedCancellationTransformer: LostBedCancellationTransformer,
) {
  fun transformJpaToApi(jpa: LostBedsEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    status = determineStatus(jpa),
    cancellation = jpa.cancellation?.let { lostBedCancellationTransformer.transformJpaToApi(it) },
    bedId = jpa.bed.id,
    bedName = jpa.bed.name,
    roomName = jpa.bed.room.name,
  )

  private fun determineStatus(jpa: LostBedsEntity) = when {
    jpa.cancellation != null -> LostBedStatus.CANCELLED
    else -> LostBedStatus.ACTIVE
  }
}
