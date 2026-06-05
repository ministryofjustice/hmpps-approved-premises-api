package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationsTransformer

@Cas2HdcExternalController
class Cas2HdcExternalReferralController(
  private val cas2HdcApplicationService: Cas2HdcApplicationService,
  private val cas2HdcApplicationsTransformer: Cas2HdcApplicationsTransformer,

) {

  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<List<Cas2ReferralHistory>> = ResponseEntity.ok(
    cas2HdcApplicationService.getSubmittedApplicationsByCrn(crn).map {
      cas2HdcApplicationsTransformer.transformJpaToCas2HdcReferralHistory(it)
    },
  )
}
