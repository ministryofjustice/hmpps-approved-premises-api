package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceStatus

@Component
class Cas3VoidBedspacesTransformer(
  private val cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer,
) {

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
    costCentre = entity.costCentre,
  )
}
