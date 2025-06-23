package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

@Component
class Cas3BedspaceTransformer(
  private val characteristicTransformer: CharacteristicTransformer,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun transformJpaToApi(jpa: RoomEntity): Cas3Bedspace? {
    val bed = jpa.beds.firstOrNull()

    if (bed == null) {
      log.error("No beds found for room ID ${jpa.id}.")
      return null
    }
    return Cas3Bedspace(
      id = bed.id,
      reference = jpa.name,
      startDate = bed.startDate,
      endDate = bed.endDate,
      status = bed.getCas3BedspaceStatus(),
      notes = jpa.notes,
      characteristics = jpa.characteristics.map(characteristicTransformer::transformJpaToApi),
    )
  }

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
