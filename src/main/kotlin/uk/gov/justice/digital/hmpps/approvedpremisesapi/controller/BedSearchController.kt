package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BedsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchResultTransformer

@Service
class BedSearchController(
  private val userService: UserService,
  private val bedSearchService: BedSearchService,
  private val bedSearchResultTransformer: BedSearchResultTransformer,
) : BedsApiDelegate {
  override fun bedsSearchPost(bedSearchParameters: BedSearchParameters): ResponseEntity<BedSearchResults> {
    val user = userService.getUserForRequest()

    val authorisationResult = when (bedSearchParameters) {
      is ApprovedPremisesBedSearchParameters -> bedSearchService.findApprovedPremisesBeds(
        user = user,
        maxDistanceMiles = bedSearchParameters.maxDistanceMiles,
        startDate = bedSearchParameters.startDate,
        durationInDays = bedSearchParameters.durationDays,
        requiredCharacteristics = bedSearchParameters.requiredCharacteristics,
        postcodeDistrictOutcode = bedSearchParameters.postcodeDistrict,
      )
      is TemporaryAccommodationBedSearchParameters -> bedSearchService.findTemporaryAccommodationBeds(
        user = user,
        probationDeliveryUnit = bedSearchParameters.probationDeliveryUnit,
        startDate = bedSearchParameters.startDate,
        durationInDays = bedSearchParameters.durationDays,
        filterBySharedProperty = bedSearchParameters.sharedProperty == true,
        filterBySingleOccupancy = bedSearchParameters.singleOccupancy == true,
      )
      else -> throw RuntimeException("Unsupported BedSearchParameters type: ${bedSearchParameters::class.qualifiedName}")
    }

    val validationResult = when (authorisationResult) {
      is AuthorisableActionResult.Success -> authorisationResult.entity
      else -> throw ForbiddenProblem()
    }

    val results = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(
      bedSearchResultTransformer.transformDomainToApi(results),
    )
  }
}
