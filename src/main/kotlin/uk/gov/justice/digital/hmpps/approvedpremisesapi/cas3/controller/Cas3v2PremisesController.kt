package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2PremisesController(
  private val cas3UserAccessService: Cas3UserAccessService,
  private val cas3PremisesService: Cas3PremisesService,
  private val cas3v2PremisesService: Cas3v2PremisesService,
  private val cas3PremisesTransformer: Cas3PremisesTransformer,
  private val cas3PremisesSearchResultsTransformer: Cas3PremisesSearchResultsTransformer,
  private val userService: UserService,
) {

  @GetMapping("/premises/{premisesId}")
  fun getPremises(@PathVariable premisesId: UUID): ResponseEntity<Cas3Premises> {
    val premises = extractEntityFromCasResult(cas3v2PremisesService.getValidatedPremises(premisesId))
    val archiveHistory = extractEntityFromCasResult(cas3PremisesService.getPremisesArchiveHistory(premises.id))
    return ResponseEntity.ok(cas3PremisesTransformer.toCas3Premises(premises, archiveHistory))
  }

  @GetMapping("/premises/search")
  fun premisesSearch(
    @RequestParam postcodeOrAddress: String?,
    @RequestParam premisesStatus: Cas3PremisesStatus,
  ): ResponseEntity<Cas3PremisesSearchResults> {
    val user = userService.getUserForRequest()
    val premisesSummaries = cas3v2PremisesService.getAllPremisesSummaries(user.probationRegion.id, postcodeOrAddress, premisesStatus).groupBy { it.id }
    val premisesSearchResults = cas3PremisesSearchResultsTransformer.transformDomainToCas3PremisesSearchResults(premisesSummaries)
    val sortedResults = Cas3PremisesSearchResults(
      results = premisesSearchResults.results?.sortedBy { it.id },
      totalPremises = premisesSearchResults.totalPremises,
      totalOnlineBedspaces = premisesSearchResults.totalOnlineBedspaces,
      totalUpcomingBedspaces = premisesSearchResults.totalUpcomingBedspaces,
    )

    return ResponseEntity.ok(sortedResults)
  }

  @PostMapping("/premises")
  fun createPremises(@RequestBody body: Cas3NewPremises): ResponseEntity<Cas3Premises> {
    if (!cas3UserAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val premises = extractEntityFromCasResult(
      cas3v2PremisesService.createNewPremises(
        reference = body.reference,
        addressLine1 = body.addressLine1,
        addressLine2 = body.addressLine2,
        town = body.town,
        postcode = body.postcode,
        localAuthorityAreaId = body.localAuthorityAreaId,
        probationRegionId = body.probationRegionId,
        probationDeliveryUnitId = body.probationDeliveryUnitId,
        characteristicIds = body.characteristicIds,
        notes = body.notes,
        turnaroundWorkingDays = body.turnaroundWorkingDays,
      ),
    )

    return ResponseEntity(
      cas3PremisesTransformer.toCas3Premises(premises),
      HttpStatus.CREATED,
    )
  }

  @PutMapping("/premises/{premisesId}")
  fun updatePremises(
    @PathVariable premisesId: UUID,
    @RequestBody body: Cas3UpdatePremises,
  ): ResponseEntity<Cas3Premises> {
    if (!cas3UserAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val premises = cas3v2PremisesService.updatePremises(
      premisesId = premisesId,
      addressLine1 = body.addressLine1,
      addressLine2 = body.addressLine2,
      town = body.town,
      postcode = body.postcode,
      localAuthorityAreaId = body.localAuthorityAreaId,
      probationRegionId = body.probationRegionId,
      characteristicIds = body.characteristicIds,
      notes = body.notes,
      probationDeliveryUnitId = body.probationDeliveryUnitId,
      turnaroundWorkingDays = body.turnaroundWorkingDays!!,
      reference = body.reference,
    )

    return ResponseEntity.ok(cas3PremisesTransformer.toCas3Premises(extractEntityFromCasResult(premises)))
  }
}
