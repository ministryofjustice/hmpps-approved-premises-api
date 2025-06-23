package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
) {
  fun transformJpaToApi(bed: BedEntity) = Cas3Bedspace(
    id = bed.id,
    reference = bed.room.name,
    startDate = bed.startDate,
    endDate = bed.endDate,
    status = bed.getCas3BedspaceStatus(),
    notes = bed.room.notes,
    characteristics = bed.room.characteristics.map(characteristicTransformer::transformJpaToApi),
  )

  fun transformJpaToApi(jpa: Cas3BedspacesEntity) = Bed(
    id = jpa.id,
    name = jpa.reference,
    code = null,
    bedEndDate = jpa.endDate,
  )
}
