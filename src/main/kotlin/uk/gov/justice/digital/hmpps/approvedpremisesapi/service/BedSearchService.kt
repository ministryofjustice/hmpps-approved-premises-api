package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.LocalDate
import java.util.UUID

@Service
class BedSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicService: CharacteristicService,
) {
  fun findApprovedPremisesBeds(
    user: UserEntity,
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInWeeks: Int,
    requiredCharacteristics: List<String>,
  ): AuthorisableActionResult<ValidatableActionResult<List<ApprovedPremisesBedSearchResult>>> {
    if (!user.hasRole(UserRole.MATCHER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val characteristicErrors = mutableListOf<String>()
        val premisesCharacteristicIds = mutableListOf<UUID>()
        val roomCharacteristicIds = mutableListOf<UUID>()

        val characteristics = characteristicService.getCharacteristicsByPropertyNames(requiredCharacteristics)

        requiredCharacteristics.forEach { propertyName ->
          val characteristic = characteristics.firstOrNull { it.propertyName == propertyName }
          when {
            characteristic == null -> characteristicErrors += "$propertyName doesNotExist"
            characteristic.matches(ServiceName.approvedPremises.value, "premises") -> premisesCharacteristicIds += characteristic.id
            characteristic.matches(ServiceName.approvedPremises.value, "room") -> roomCharacteristicIds += characteristic.id
            else -> characteristicErrors += "$propertyName scopeInvalid"
          }
        }

        if (characteristicErrors.any()) {
          "$.requiredCharacteristics" hasValidationError characteristicErrors.joinToString(", ")
        }

        postcodeDistrictRepository.findByOutcode(postcodeDistrictOutcode)
          ?: ("$.postcodeDistrictOutcode" hasValidationError "doesNotExist")

        if (durationInWeeks < 1) {
          "$.durationInWeeks" hasValidationError "mustBeAtLeast1"
        }

        if (maxDistanceMiles < 1) {
          "$.maxDistanceMiles" hasValidationError "mustBeAtLeast1"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        return@validated success(
          bedSearchRepository.findApprovedPremisesBeds(
            postcodeDistrictOutcode = postcodeDistrictOutcode,
            maxDistanceMiles = maxDistanceMiles,
            startDate = startDate,
            durationInWeeks = durationInWeeks,
            requiredPremisesCharacteristics = premisesCharacteristicIds,
            requiredRoomCharacteristics = roomCharacteristicIds,
          ),
        )
      },
    )
  }

  fun findTemporaryAccommodationBeds(
    user: UserEntity,
    probationDeliveryUnit: String,
    startDate: LocalDate,
    durationInDays: Int,
  ): AuthorisableActionResult<ValidatableActionResult<List<TemporaryAccommodationBedSearchResult>>> {
    return AuthorisableActionResult.Success(
      validated {
        if (durationInDays < 1) {
          "$.durationDays" hasValidationError "mustBeAtLeast1"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        return@validated success(
          bedSearchRepository.findTemporaryAccommodationBeds(
            probationDeliveryUnit = probationDeliveryUnit,
            startDate = startDate,
            durationInDays = durationInDays,
            probationRegionId = user.probationRegion.id
          ),
        )
      },
    )
  }
}
