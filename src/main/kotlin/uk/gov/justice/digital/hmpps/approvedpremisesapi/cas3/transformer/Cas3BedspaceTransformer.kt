package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSummary
import java.time.LocalDate

@Component
class Cas3BedspaceTransformer(
  private val cas3BedspaceCharacteristicTransformer: Cas3BedspaceCharacteristicTransformer,
) {
  fun transformJpaToApi(jpa: Cas3BedspacesEntity, status: Cas3BedspaceStatus, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = jpa.id,
    reference = jpa.reference,
    startDate = jpa.createdDate,
    endDate = jpa.endDate,
    scheduleUnarchiveDate = isBedspaceScheduledToUnarchive(jpa),
    notes = jpa.notes,
    status = status,
    bedspaceCharacteristics = jpa.characteristics.map(cas3BedspaceCharacteristicTransformer::transformJpaToApi),
    archiveHistory = archiveHistory,
  )

  fun transformJpaToCas3BedspaceSummary(jpa: Cas3BedspacesEntity) = Cas3BedspaceSummary(
    id = jpa.id,
    reference = jpa.reference,
    endDate = jpa.endDate,
  )

  fun isBedspaceScheduledToUnarchive(bedspace: Cas3BedspacesEntity) = bedspace.startDate?.takeIf { it.isAfter(LocalDate.now()) }
}
