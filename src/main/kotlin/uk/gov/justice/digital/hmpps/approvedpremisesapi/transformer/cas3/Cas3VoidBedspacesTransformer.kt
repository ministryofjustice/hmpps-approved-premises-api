package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesEntity

@Component
class Cas3VoidBedspacesTransformer(
  private val cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer,
  private val cas3VoidBedspaceCancellationTransformer: Cas3VoidBedspaceCancellationTransformer,
) {
  fun transformJpaToApi(jpa: Cas3VoidBedspacesEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    reason = cas3VoidBedspaceReasonTransformer.transformJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    status = determineStatus(jpa),
    cancellation = jpa.cancellation?.let { cas3VoidBedspaceCancellationTransformer.transformJpaToApi(it) },
    bedId = jpa.bed.id,
    bedName = jpa.bed.name,
    roomName = jpa.bed.room.name,
  )

  private fun determineStatus(jpa: Cas3VoidBedspacesEntity) = when {
    jpa.cancellation != null -> LostBedStatus.cancelled
    else -> LostBedStatus.active
  }
}
