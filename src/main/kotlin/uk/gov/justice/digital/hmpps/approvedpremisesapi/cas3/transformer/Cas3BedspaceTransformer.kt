package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import java.time.LocalDate

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
  private val cas3BedspaceCharacteristicTransformer: Cas3BedspaceCharacteristicTransformer,
) {
  fun transformJpaToApi(bedspace: BedEntity, status: Cas3BedspaceStatus, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = bedspace.id,
    reference = bedspace.room.name,
    startDate = bedspace.createdDate,
    endDate = bedspace.endDate,
    scheduleUnarchiveDate = isBedspaceScheduledToUnarchive(bedspace),
    status = status,
    notes = bedspace.room.notes,
    characteristics = bedspace.room.characteristics.map(characteristicTransformer::transformJpaToApi),
    archiveHistory = archiveHistory,
  )

  fun transformJpaToApi(jpa: Cas3BedspacesEntity, status: Cas3BedspaceStatus, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = jpa.id,
    reference = jpa.reference,
    startDate = jpa.createdAt.toLocalDate(),
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

  fun isBedspaceScheduledToUnarchive(bedspace: BedEntity) = bedspace.startDate?.takeIf { it.isAfter(LocalDate.now()) }
  fun isBedspaceScheduledToUnarchive(bedspace: Cas3BedspacesEntity) = bedspace.startDate?.takeIf { it.isAfter(LocalDate.now()) }
}
