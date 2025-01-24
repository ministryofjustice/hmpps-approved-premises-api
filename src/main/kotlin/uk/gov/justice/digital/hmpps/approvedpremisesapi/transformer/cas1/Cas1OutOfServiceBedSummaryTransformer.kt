package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class Cas1OutOfServiceBedSummaryTransformer(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
) {

  fun toCas1OutOfServiceBedSummary(jpa: Cas1OutOfServiceBedEntity) = Cas1OutOfServiceBedSummary(
    id = jpa.id,
    bedId = jpa.bed.id,
    roomName = jpa.bed.room.name,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    reason = cas1OutOfServiceBedReasonTransformer.transformJpaToApi(jpa.reason),
    characteristics = jpa.bed.room.characteristics.map { it.asCas1SpaceCharacteristic() },
  )

  private fun CharacteristicEntity.asCas1SpaceCharacteristic() =
    Cas1SpaceCharacteristic.entries.first { it.value == this.propertyName }
}
