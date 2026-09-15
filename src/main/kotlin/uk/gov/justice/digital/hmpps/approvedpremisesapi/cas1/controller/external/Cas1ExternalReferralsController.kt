package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer

@Cas1ExternalController
class Cas1ExternalReferralsController(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val cas1ExternalApplicationService: Cas1ExternalApplicationService,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(@PathVariable crn: String): ResponseEntity<List<Cas1ReferralHistory>> = ResponseEntity.ok(
    approvedPremisesApplicationRepository.findByCrn(crn).flatMap { application ->
      val placementHistory = cas1ExternalApplicationService.getPlacementPairs(application.id)
      cas1AssessmentTransformer.transformDomainToApiCas1ReferralHistory(application, placementHistory)
    },
  )
}
