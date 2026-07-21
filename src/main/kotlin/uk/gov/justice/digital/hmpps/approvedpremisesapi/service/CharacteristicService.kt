package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import java.util.UUID

@Service
class CharacteristicService(
  val characteristicRepository: CharacteristicRepository,
  val bedspaceCharacteristicRepository: Cas3BedspaceCharacteristicRepository,
  val premisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
) {
  fun getCharacteristicsByPropertyNames(requiredCharacteristics: List<String>, serviceName: ServiceName) = characteristicRepository.findAllWherePropertyNameIn(requiredCharacteristics, serviceName.value)

  fun serviceScopeMatches(characteristic: CharacteristicEntity, target: Any): Boolean {
    val targetService = getServiceForTarget(target) ?: return false

    return when (characteristic.serviceScope) {
      "*" -> true
      targetService -> true
      else -> return false
    }
  }

  fun getCas3BedspaceCharacteristic(characteristicId: UUID): Cas3BedspaceCharacteristicEntity? = bedspaceCharacteristicRepository.findByIdOrNull(characteristicId)

  fun getCas3BedspaceCharacteristics() = bedspaceCharacteristicRepository.findByActive(active = true)

  fun getCas3PremisesCharacteristics() = premisesCharacteristicRepository.findByActive(active = true)

  fun modelScopeMatches(characteristic: CharacteristicEntity, target: Any): Boolean = when (characteristic.modelScope) {
    "*" -> true
    "room" -> target is RoomEntity
    "premises" -> target is PremisesEntity
    else -> false
  }

  private fun getServiceForTarget(target: Any): String? = when (target) {
    is RoomEntity -> getServiceForTarget(target.premises)
    is ApprovedPremisesEntity -> ServiceName.approvedPremises.value
    else -> null
  }
}
