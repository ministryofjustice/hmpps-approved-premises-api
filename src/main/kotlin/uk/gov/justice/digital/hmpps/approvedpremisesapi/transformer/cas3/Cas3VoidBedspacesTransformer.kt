package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity

@Component
class Cas3VoidBedspacesTransformer(
  private val cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer,
  private val cas3VoidBedspaceCancellationTransformer: Cas3VoidBedspaceCancellationTransformer,
) {
  fun transformJpaToApi(jpa: Cas3VoidBedspaceEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    reason = cas3VoidBedspaceReasonTransformer.transformJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    status = determineStatus(jpa),
    cancellation = jpa.cancellation?.let { cas3VoidBedspaceCancellationTransformer.transformJpaToApi(it) },
    bedId = jpa.bed!!.id,
    bedName = jpa.bed!!.name,
    roomName = jpa.bed!!.room.name,
  )

  private fun determineStatus(jpa: Cas3VoidBedspaceEntity) = when {
    jpa.cancellation != null -> LostBedStatus.cancelled
    else -> LostBedStatus.active
  }

  fun toCas3VoidBedspace(entity: Cas3VoidBedspaceEntity): Cas3VoidBedspace = Cas3VoidBedspace(
    id = entity.id,
    startDate = entity.startDate,
    endDate = entity.endDate,
    reason = cas3VoidBedspaceReasonTransformer.toCas3VoidBedspaceReason(entity.reason),
    referenceNumber = entity.referenceNumber,
    notes = entity.notes,
    bedspaceId = entity.bedspace!!.id,
    bedspaceName = entity.bedspace!!.reference,
    cancellationDate = entity.cancellationDate?.toLocalDate(),
    cancellationNotes = entity.cancellationNotes,
    status = if (entity.cancellationDate == null) Cas3VoidBedspaceStatus.ACTIVE else Cas3VoidBedspaceStatus.CANCELLED,
  )
}
