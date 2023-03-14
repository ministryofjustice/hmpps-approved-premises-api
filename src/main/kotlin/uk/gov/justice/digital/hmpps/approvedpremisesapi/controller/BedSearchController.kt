package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BedsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchTransformer

@Service
class BedSearchController(
  private val bedSearchService: BedSearchService,
  private val userService: UserService,
  private val bedSearchTransformer: BedSearchTransformer
) : BedsApiDelegate {
  override fun bedsSearchPost(xServiceName: ServiceName, bedSearchParameters: BedSearchParameters): ResponseEntity<List<BedSearchResult>> {
    val authorisationResult = bedSearchService.findBeds(
      user = userService.getUserForRequest(),
      postcodeDistrictOutcode = bedSearchParameters.postcodeDistrict,
      maxDistanceMiles = bedSearchParameters.maxDistanceMiles ?: 50,
      startDate = bedSearchParameters.startDate,
      durationInDays = bedSearchParameters.durationDays,
      requiredPremisesCharacteristics = bedSearchParameters.requiredPremisesCharacteristics,
      requiredRoomCharacteristics = listOf(),
      service = xServiceName.value
    )

    val validationResult = when (authorisationResult) {
      is AuthorisableActionResult.Success -> authorisationResult.entity
      else -> throw ForbiddenProblem()
    }

    val searchResults = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(
      bedSearchTransformer.domainToApi(searchResults)
    )
  }
}
