package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
  private val cas3BedspaceCharacteristicTransformer: Cas3BedspaceCharacteristicTransformer,
) {
  fun transformJpaToApi(bed: BedEntity) = Cas3Bedspace(
    id = bed.id,
    reference = bed.room.name,
    startDate = bed.startDate!!,
    endDate = bed.endDate,
    status = bed.getCas3BedspaceStatus(),
    notes = bed.room.notes,
    characteristics = bed.room.characteristics.map(characteristicTransformer::transformJpaToApi),
  )

  fun transformJpaToApi(jpa: Cas3BedspacesEntity) = Cas3Bedspace(
    id = jpa.id,
    reference = jpa.reference,
    startDate = jpa.startDate!!,
    endDate = jpa.endDate,
    notes = jpa.notes,
    status = Cas3BedspaceStatus.online, // sets online as default for now - will change when we get there
    bedspaceCharacteristics = jpa.characteristics.map(cas3BedspaceCharacteristicTransformer::transformJpaToApi),
  )
}
