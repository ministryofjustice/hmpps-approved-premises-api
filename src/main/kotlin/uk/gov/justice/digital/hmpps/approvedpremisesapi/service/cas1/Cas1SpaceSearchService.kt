package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

@Service
class Cas1SpaceSearchService(
  private val characteristicService: CharacteristicService,
  private val spaceSearchRepository: Cas1SpaceSearchRepository,
  private val applicationRepository: ApprovedPremisesApplicationRepository,
) {
  fun findSpaces(searchParameters: Cas1SpaceSearchParameters): List<CandidatePremises> {
    val applicationId = searchParameters.applicationId
    val application = applicationRepository.findByIdOrNull(searchParameters.applicationId)
      ?: throw NotFoundProblem(applicationId, "Application")

    val requiredCharacteristics = getRequiredCharacteristics(searchParameters)

    return spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
      targetPostcodeDistrict = searchParameters.targetPostcodeDistrict,
      isWomensPremises = application.isWomensApplication!!,
      premisesCharacteristics = requiredCharacteristics.groupedCharacteristics.premisesCharacteristics,
      roomCharacteristics = requiredCharacteristics.groupedCharacteristics.roomCharacteristics,
    )
  }

  private fun getRequiredCharacteristics(parameters: Cas1SpaceSearchParameters): RequiredCharacteristics {
    val requirements = parameters.requirements
    return RequiredCharacteristics(
      groupedCharacteristics = getSpaceCharacteristics(parameters),
    )
  }

  private fun getSpaceCharacteristics(parameters: Cas1SpaceSearchParameters): GroupedCharacteristics {
    val requirements = parameters.requirements
    val propertyNames =
      (
        (requirements.spaceCharacteristics?.map { it.value } ?: listOf()) +
          (parameters.spaceCharacteristics?.map { it.value } ?: listOf())
        ).toSet()
    val characteristics = characteristicService.getCharacteristicsByPropertyNames(propertyNames.toList(), ServiceName.approvedPremises)

    return GroupedCharacteristics(
      characteristics.filter { it.isPremisesCharacteristic() }.map { it.id },
      characteristics.filter { it.isRoomCharacteristic() }.map { it.id },
    )
  }

  private fun CharacteristicEntity.isPremisesCharacteristic(): Boolean = this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("premises")

  private fun CharacteristicEntity.isRoomCharacteristic(): Boolean = this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("room")
}

data class RequiredCharacteristics(
  val groupedCharacteristics: GroupedCharacteristics,
)

data class GroupedCharacteristics(
  val premisesCharacteristics: List<UUID>,
  val roomCharacteristics: List<UUID>,
)
