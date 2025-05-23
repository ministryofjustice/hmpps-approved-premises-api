package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
) {
  fun transformJpaToApi(jpa: RoomEntity): Cas3Bedspace {
    val bed = jpa.beds.firstOrNull() ?: error("No beds in room ${jpa.id}.")
    return Cas3Bedspace(
      id = bed.id,
      reference = jpa.name,
      // replace this with start date when https://dsdmoj.atlassian.net/browse/CAS-1627 is complete
      startDate = bed.createdAt?.toLocalDate()!!,
      endDate = bed.endDate,
      notes = jpa.notes,
      characteristics = jpa.characteristics.map(characteristicTransformer::transformJpaToApi),
    )
  }
}
