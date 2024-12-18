package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremiseApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.zipBy
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1SpaceSearchService(
  private val characteristicService: CharacteristicService,
  private val spaceSearchRepository: Cas1SpaceSearchRepository,
  private val applicationRepository: ApprovedPremiseApplicationRepository,
) {
  fun findSpaces(searchParameters: Cas1SpaceSearchParameters): List<Cas1SpaceSearchResult> {
    val applicationId = searchParameters.applicationId
    val application = applicationRepository.findByIdOrNull(searchParameters.applicationId)
      ?: throw NotFoundProblem(applicationId, "Application")

    val requiredCharacteristics = getRequiredCharacteristics(searchParameters)

    val candidatePremises = getCandidatePremises(
      searchParameters.targetPostcodeDistrict,
      requiredCharacteristics,
      isWomensPremises = application.isWomensApplication!!,
    )

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
    getSpaceCharacteristics(searchParameters),
  )

  private fun getSpaceCharacteristics(searchParameters: Cas1SpaceSearchParameters): CharacteristicGroup {
    val characteristicNames = searchParameters.requirements.spaceCharacteristics?.map { it.value } ?: listOf()

    return getCharacteristicGroup(characteristicNames)
  }

  private fun getCharacteristicGroup(characteristicNames: List<String?>): CharacteristicGroup {
    val characteristics = characteristicService.getCharacteristicsByPropertyNames(characteristicNames.filterNotNull(), ServiceName.approvedPremises)

    return CharacteristicGroup(
      characteristics.filter { it.isPremisesCharacteristic() }.map { it.id },
      characteristics.filter { it.isRoomCharacteristic() }.map { it.id },
    )
  }

  private fun getCandidatePremises(
    targetPostcodeDistrict: String,
    requiredCharacteristics: RequiredCharacteristics,
    isWomensPremises: Boolean,
  ): List<CandidatePremises> {
    return spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
      targetPostcodeDistrict,
      requiredCharacteristics.apTypes,
      isWomensPremises,
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
