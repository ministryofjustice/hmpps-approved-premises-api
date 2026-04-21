package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer

@Cas1Controller
@Tag(name = "CAS1 Space Search")
class Cas1SpaceSearchController(
  private val spaceSearchService: Cas1PremisesSearchService,
  private val spaceSearchResultTransformer: Cas1SpaceSearchResultsTransformer,
  private val userAccessService: Cas1UserAccessService,
  private val cas1ApplicationService: Cas1ApplicationService,
) {

  @Operation(
    tags = ["space searches"],
    summary = "Search for accommodation \"spaces\" which are available and match the given requirements",
    operationId = "spaceSearch",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1SpaceSearchResults::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/spaces/search"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun spaceSearch(@RequestBody cas1SpaceSearchParameters: Cas1SpaceSearchParameters): ResponseEntity<Cas1SpaceSearchResults> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_CREATE)

    val applicationId = cas1SpaceSearchParameters.applicationId
    val application = cas1ApplicationService.getApplication(applicationId)
      ?: throw BadRequestProblem(errorDetail = "Cannot find application with ID $applicationId")

    val results = spaceSearchService.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        gender = if (application.isWomensApplication == true) {
          ApprovedPremisesGender.WOMAN
        } else {
          ApprovedPremisesGender.MAN
        },
        cas1SpaceSearchParameters.targetPostcodeDistrict,
        cas1SpaceSearchParameters.spaceCharacteristics?.toSet() ?: emptySet(),
      ),
    )

    return ResponseEntity.ok(spaceSearchResultTransformer.transformDomainToApi(results))
  }
}
