package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity

@Component
class RoomTransformer(
  private val bedTransformer: BedTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
) {
  fun transformJpaToApi(jpa: RoomEntity) = Room(
    id = jpa.id,
    name = jpa.name,
    notes = jpa.notes ?: "",
    beds = jpa.beds.map(bedTransformer::transformJpaToApi),
    characteristics = jpa.characteristics.map(characteristicTransformer::transformJpaToApi),
  )
}
