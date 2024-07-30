package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.zipBy
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1SpaceSearchService(
  private val characteristicService: CharacteristicService,
  private val spaceSearchRepository: Cas1SpaceSearchRepository,
) {
  fun findSpaces(searchParameters: Cas1SpaceSearchParameters): List<Cas1SpaceSearchResult> {
    val requiredCharacteristics = getRequiredCharacteristics(searchParameters)

    val candidatePremises = getCandidatePremises(searchParameters.targetPostcodeDistrict, requiredCharacteristics)

    if (candidatePremises.isEmpty()) {
      return emptyList()
    }

    val availability = getAvailabilityForCandidatePremises(
      candidatePremises,
      searchParameters.startDate,
      searchParameters.durationInDays,
    )

    return candidatePremises
      .zipBy(availability, CandidatePremises::premisesId, SpaceAvailability::premisesId)
      .map(Cas1SpaceSearchResult::fromPair)
  }

  private fun getRequiredCharacteristics(searchParameters: Cas1SpaceSearchParameters) = RequiredCharacteristics(
    searchParameters.requirements.apTypes?.map { it.asApprovedPremisesType() } ?: listOf(),
    getGenderCharacteristics(searchParameters),
    getSpaceCharacteristics(searchParameters),
  )

  private fun getGenderCharacteristics(searchParameters: Cas1SpaceSearchParameters): List<UUID> {
    val characteristicNames = mutableListOf<String?>()

    searchParameters.requirements.genders?.forEach {
      characteristicNames += when (it) {
        Gender.male -> null
        Gender.female -> null
      }
    }

    return getCharacteristicGroup(characteristicNames).premisesCharacteristics
  }

  private fun getSpaceCharacteristics(searchParameters: Cas1SpaceSearchParameters): CharacteristicGroup {
    val characteristicNames = searchParameters.requirements.spaceCharacteristics?.map { it.value } ?: listOf()

    return getCharacteristicGroup(characteristicNames)
  }

  private fun getCharacteristicGroup(characteristicNames: List<String?>): CharacteristicGroup {
    val characteristics = characteristicService.getCharacteristicsByPropertyNames(characteristicNames.filterNotNull())

    return CharacteristicGroup(
      characteristics.filter { it.isPremisesCharacteristic() }.map { it.id },
      characteristics.filter { it.isRoomCharacteristic() }.map { it.id },
    )
  }

  private fun getCandidatePremises(
    targetPostcodeDistrict: String,
    requiredCharacteristics: RequiredCharacteristics,
  ): List<CandidatePremises> {
    return spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
      targetPostcodeDistrict,
      requiredCharacteristics.apTypes,
      requiredCharacteristics.genders,
      requiredCharacteristics.space.premisesCharacteristics,
      requiredCharacteristics.space.roomCharacteristics,
    )
  }

  private fun getAvailabilityForCandidatePremises(
    candidatePremises: List<CandidatePremises>,
    startDate: LocalDate,
    durationInDays: Int,
  ): List<SpaceAvailability> {
    return spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
      candidatePremises.map { it.premisesId },
      startDate,
      durationInDays,
    )
  }

  private fun CharacteristicEntity.isPremisesCharacteristic(): Boolean =
    this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("premises")

  private fun CharacteristicEntity.isRoomCharacteristic(): Boolean =
    this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("room")
}

data class RequiredCharacteristics(
  val apTypes: List<ApprovedPremisesType>,
  val genders: List<UUID>,
  val space: CharacteristicGroup,
)

data class CharacteristicGroup(
  val premisesCharacteristics: List<UUID>,
  val roomCharacteristics: List<UUID>,
)

data class Cas1SpaceSearchResult(
  val candidatePremises: CandidatePremises,
  val spaceAvailability: SpaceAvailability,
) {
  companion object {
    fun fromPair(pair: Pair<CandidatePremises, SpaceAvailability>) = Cas1SpaceSearchResult(
      pair.first,
      pair.second,
    )
  }
}
