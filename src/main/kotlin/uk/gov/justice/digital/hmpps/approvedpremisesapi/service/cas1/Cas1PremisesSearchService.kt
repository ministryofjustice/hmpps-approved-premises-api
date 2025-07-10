package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

@Service
class Cas1PremisesSearchService(
  private val characteristicService: CharacteristicService,
  private val spaceSearchRepository: Cas1SpaceSearchRepository,
) {
  data class Cas1PremisesSearchCriteria(
    val gender: ApprovedPremisesGender?,
    val targetPostcodeDistrict: String?,
    val spaceCharacteristics: List<Cas1SpaceCharacteristic>,
  )

  fun findPremises(searchCriteria: Cas1PremisesSearchCriteria): List<CandidatePremises> {
    val groupedCharacteristics = getGroupedCharacteristics(searchCriteria)

    return spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
      targetPostcodeDistrict = searchCriteria.targetPostcodeDistrict,
      gender = searchCriteria.gender,
      premisesCharacteristics = groupedCharacteristics.premisesCharacteristics,
      roomCharacteristics = groupedCharacteristics.roomCharacteristics,
    )
  }

  private fun getGroupedCharacteristics(parameters: Cas1PremisesSearchCriteria): GroupedCharacteristics {
    val propertyNames = parameters.spaceCharacteristics.map { it.value }.toSet()
    val characteristics = characteristicService.getCharacteristicsByPropertyNames(propertyNames.toList(), ServiceName.approvedPremises)

    return GroupedCharacteristics(
      characteristics.filter { it.isModelScopePremises() }.map { it.id },
      characteristics.filter { it.isModelScopeRoom() }.map { it.id },
    )
  }
}

private data class GroupedCharacteristics(
  val premisesCharacteristics: List<UUID>,
  val roomCharacteristics: List<UUID>,
)
