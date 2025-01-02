package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate
import java.util.UUID

@Service
class BedSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicService: CharacteristicService,
) {
  object Constants {
    const val MAX_NUMBER_PDUS = 5
  }

  fun findApprovedPremisesBeds(
    user: UserEntity,
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInDays: Int,
    requiredCharacteristics: List<PlacementCriteria>,
  ): CasResult<List<ApprovedPremisesBedSearchResult>> {
    if (!user.hasRole(UserRole.CAS1_MATCHER)) {
      return CasResult.Unauthorised()
    }

    return validatedCasResult {
      val characteristicErrors = mutableListOf<String>()
      val premisesCharacteristicIds = mutableListOf<UUID>()
      val roomCharacteristicIds = mutableListOf<UUID>()
      val requiredPropertyNames = requiredCharacteristics.map { it.toString() }

      val characteristics =
        characteristicService.getCharacteristicsByPropertyNames(requiredPropertyNames, ServiceName.approvedPremises)

      requiredPropertyNames.forEach { propertyName ->
        val characteristic = characteristics.firstOrNull { it.propertyName == propertyName }
        when {
          characteristic == null -> characteristicErrors += "$propertyName doesNotExist"
          characteristic.matches(
            ServiceName.approvedPremises.value,
            "premises",
          ) -> premisesCharacteristicIds += characteristic.id

          characteristic.matches(
            ServiceName.approvedPremises.value,
            "room",
          ) -> roomCharacteristicIds += characteristic.id

          else -> characteristicErrors += "$propertyName scopeInvalid"
        }
      }

      if (characteristicErrors.any()) {
        "$.requiredCharacteristics" hasValidationError characteristicErrors.joinToString(", ")
      }

      postcodeDistrictRepository.findByOutcode(postcodeDistrictOutcode)
        ?: ("$.postcodeDistrictOutcode" hasValidationError "doesNotExist")

      if (durationInDays < 1) {
        "$.durationDays" hasValidationError "mustBeAtLeast1"
      }

      if (maxDistanceMiles < 1) {
        "$.maxDistanceMiles" hasValidationError "mustBeAtLeast1"
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      return success(
        bedSearchRepository.findApprovedPremisesBeds(
          postcodeDistrictOutcode = postcodeDistrictOutcode,
          maxDistanceMiles = maxDistanceMiles,
          startDate = startDate,
          durationInDays = durationInDays,
          requiredPremisesCharacteristics = premisesCharacteristicIds,
          requiredRoomCharacteristics = roomCharacteristicIds,
        ),
      )
    }
  }
}
