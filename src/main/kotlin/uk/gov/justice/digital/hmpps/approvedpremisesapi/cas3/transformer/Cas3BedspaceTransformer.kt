package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
  private val cas3BedspaceCharacteristicTransformer: Cas3BedspaceCharacteristicTransformer,
) {
  fun transformJpaToApi(bed: BedEntity, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = bed.id,
    reference = bed.room.name,
    startDate = bed.createdAt!!.toLocalDate(),
    endDate = bed.endDate,
    status = bed.getCas3BedspaceStatus(),
    notes = bed.room.notes,
    characteristics = bed.room.characteristics.map(characteristicTransformer::transformJpaToApi),
    archiveHistory = archiveHistory,
  )

  fun transformJpaToApi(jpa: Cas3BedspacesEntity, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = jpa.id,
    reference = jpa.reference,
    startDate = jpa.createdAt!!.toLocalDate(),
    endDate = jpa.endDate,
    notes = jpa.notes,
    status = jpa.getBedspaceStatus(),
    bedspaceCharacteristics = jpa.characteristics.map(cas3BedspaceCharacteristicTransformer::transformJpaToApi),
    archiveHistory = archiveHistory,
  )
}
