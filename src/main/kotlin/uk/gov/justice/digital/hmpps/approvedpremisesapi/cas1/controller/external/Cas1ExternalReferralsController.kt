package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ReferralHistoryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer

@Cas1ExternalController
class Cas1ExternalReferralsController(
  private val cas1AssessmentService: Cas1AssessmentService,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val cas1ExternalApplicationService: Cas1ExternalApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(@PathVariable crn: String): ResponseEntity<List<Cas1ReferralHistoryDto>> = ResponseEntity.ok(
    /*
    Whilst driving this code from assessments works, it's an odd way to effectively get a list
    of submitted applications (we should just get the list of submitted applications).

    Instead, this should delegate to a function on Cas1ExternalApplicationV2Service which in turn should
    delegate to a function on Cas1RequestForPlacementOutcomeV2Service which in turn should
    delegate to a function on RequestForPlacementService that returns a list of _all_ requests
    for placements across all applications
     */
    cas1AssessmentService.getApprovedPremisesAssessmentsByCrnNotReallocated(crn).flatMap {
      val placementPairs = cas1ExternalApplicationService.getPlacementPairsNewestFirst(it.application.id)
      cas1AssessmentTransformer.transformDomainToApiCas1ReferralHistory(it, placementPairs)
    },
  )
}
