package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity

@Component
class RoomTransformer(
  private val bedTransformer: BedTransformer,
) {
  fun transformJpaToApi(jpa: RoomEntity) = Room(
    id = jpa.id,
    name = jpa.name,
    beds = jpa.beds.map(bedTransformer::transformJpaToApi)
  )
}
