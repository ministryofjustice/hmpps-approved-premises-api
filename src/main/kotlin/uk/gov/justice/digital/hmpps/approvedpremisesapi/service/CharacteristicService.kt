package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicRepository
import java.util.UUID

@Service
class CharacteristicService(
  val cas1CharacteristicRepository: Cas1CharacteristicRepository,
  val cas3BedspaceCharacteristicRepository: Cas3BedspaceCharacteristicRepository,
  val cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
) {
  fun getCas1CharacteristicsByPropertyNames(requiredCharacteristics: List<String>) = cas1CharacteristicRepository.findAllWherePropertyNameIn(requiredCharacteristics)

  fun getCas3BedspaceCharacteristic(characteristicId: UUID): Cas3BedspaceCharacteristicEntity? = cas3BedspaceCharacteristicRepository.findByIdOrNull(characteristicId)

  fun getCas3BedspaceCharacteristics() = cas3BedspaceCharacteristicRepository.findByActive(active = true)

  fun getCas3PremisesCharacteristics() = cas3PremisesCharacteristicRepository.findByActive(active = true)
}
