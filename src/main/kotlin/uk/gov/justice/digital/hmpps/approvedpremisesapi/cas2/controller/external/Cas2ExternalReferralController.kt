package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2ApplicationsTransformer

@Cas2ExternalController
class Cas2ExternalReferralController(
  private val cas2ApplicationService: Cas2ApplicationService,
  private val cas2ApplicationsTransformer: Cas2ApplicationsTransformer,

) {

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<List<Cas2ReferralHistory>> = ResponseEntity.ok(
    cas2ApplicationService.getSubmittedApplicationsByCrn(crn).map {
      cas2ApplicationsTransformer.transformJpaToCas2ReferralHistory(it)
    },
  )
}
