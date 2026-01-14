package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer

@Cas1ExternalController
class Cas1ExternalReferralsController(
  private val cas1AssessmentService: Cas1AssessmentService,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(@PathVariable crn: String): ResponseEntity<List<Cas1ReferralHistory>> = ResponseEntity.ok(
    cas1AssessmentService.getApprovedPremisesAssessmentsByCrn(crn).map {
      cas1AssessmentTransformer.transformDomainToApiCas1ReferralHistory(it)
    },
  )
}
