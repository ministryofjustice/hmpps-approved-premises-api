package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
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
    durationInDays: Int,
    requiredPremisesCharacteristics: List<String>,
    requiredRoomCharacteristics: List<String>,
  ): AuthorisableActionResult<ValidatableActionResult<List<ApprovedPremisesBedSearchResult>>> {
    if (!user.hasRole(UserRole.MATCHER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val premisesCharacteristicErrors = mutableListOf<String>()
        val premisesCharacteristics = requiredPremisesCharacteristics.mapNotNull {
          val characteristic = characteristicService.getCharacteristicByPropertyName(it)

          if (characteristic == null) {
            premisesCharacteristicErrors += "$it doesNotExist"
            return@mapNotNull null
          }

          if (!characteristic.matches(ServiceName.approvedPremises.value, "premises")) {
            premisesCharacteristicErrors += "$it scopeInvalid"
            return@mapNotNull null
          }

          characteristic
        }

        if (premisesCharacteristicErrors.any()) {
          "$.requiredPremisesCharacteristics" hasValidationError premisesCharacteristicErrors.joinToString(", ")
        }

        val roomCharacteristicErrors = mutableListOf<String>()
        val roomCharacteristics = requiredRoomCharacteristics.mapNotNull {
          val characteristic = characteristicService.getCharacteristicByPropertyName(it)

          if (characteristic == null) {
            roomCharacteristicErrors += "$it doesNotExist"
            return@mapNotNull null
          }

          if (!characteristic.matches(ServiceName.approvedPremises.value, "room")) {
            roomCharacteristicErrors += "$it scopeInvalid"
            return@mapNotNull null
          }

          characteristic
        }

        if (roomCharacteristicErrors.any()) {
          "$.requiredRoomCharacteristics" hasValidationError roomCharacteristicErrors.joinToString(", ")
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
          return@validated fieldValidationError
        }

        return@validated success(
          bedSearchRepository.findApprovedPremisesBeds(
            postcodeDistrictOutcode = postcodeDistrictOutcode,
            maxDistanceMiles = maxDistanceMiles,
            startDate = startDate,
            durationInDays = durationInDays,
            requiredPremisesCharacteristics = premisesCharacteristics.map(CharacteristicEntity::id),
            requiredRoomCharacteristics = roomCharacteristics.map(CharacteristicEntity::id),
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
