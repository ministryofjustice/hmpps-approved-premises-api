package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2ApplicationsTransformer

@Cas2v2ExternalController
class Cas2v2ExternalReferralController(
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,

) {

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<List<Cas2v2ReferralHistory>> = ResponseEntity.ok(
    cas2v2ApplicationService.getSubmittedApplicationsByCrn(crn).map {
      cas2v2ApplicationsTransformer.transformJpaToCas2v2ReferralHistory(it)
    },
  )
}
