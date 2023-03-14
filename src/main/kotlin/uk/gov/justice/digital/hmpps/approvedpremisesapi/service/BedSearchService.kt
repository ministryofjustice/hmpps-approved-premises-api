package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.LocalDate
import java.util.UUID

@Service
class BedSearchService(
  private val bedSearchRepository: BedSearchRepository,
  private val characteristicService: CharacteristicService
) {
  fun findBeds(
    user: UserEntity,
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInDays: Int,
    requiredPremisesCharacteristics: List<String>,
    requiredRoomCharacteristics: List<String>,
    service: String
  ): AuthorisableActionResult<ValidatableActionResult<List<BedSearchResult>>> {
    if (! user.hasRole(UserRole.MATCHER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val (premisesCharacteristicIds, premisesCharacteristicsErrors) = getCharacteristicIdsCollectingErrors(service, "premises", requiredPremisesCharacteristics)
        val (roomCharacteristicIds, roomCharacteristicsErrors) = getCharacteristicIdsCollectingErrors(service, "room", requiredRoomCharacteristics)

        if (premisesCharacteristicsErrors.any()) {
          "$.requiredPremisesCharacteristics" hasValidationError premisesCharacteristicsErrors.joinToString(", ")
        }

        if (roomCharacteristicsErrors.any()) {
          "$.requiredRoomCharacteristics" hasValidationError roomCharacteristicsErrors.joinToString(", ")
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        return@validated success(
          bedSearchRepository.findBeds(
            postcodeDistrictOutcode,
            maxDistanceMiles,
            startDate,
            durationInDays,
            premisesCharacteristicIds,
            roomCharacteristicIds,
            service
          )
        )
      }
    )
  }

  private fun getCharacteristicIdsCollectingErrors(serviceScope: String, modelScope: String, characteristicNames: List<String>): Pair<List<UUID>, List<String>> {
    val ids = mutableListOf<UUID>()
    val errors = mutableListOf<String>()

    characteristicNames.forEach { characteristicName ->
      val characteristic = characteristicService.getCharacteristic(characteristicName)

      if (characteristic == null) {
        errors += "$characteristicName doesNotExist"
        return@forEach
      }

      var hasScopeError = false
      if (characteristic.serviceScope != serviceScope && characteristic.serviceScope != "*") {
        errors += "$characteristicName serviceScopeInvalid"
        hasScopeError = true
      }

      if (characteristic.modelScope != modelScope && characteristic.modelScope != "*") {
        errors += "$characteristicName modelScopeInvalid"
        hasScopeError = true
      }

      if (!hasScopeError) ids += characteristic.id
    }

    return Pair(ids, errors)
  }
}
