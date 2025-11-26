package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.external

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3AssessmentTransformer

@Cas3ExternalController
class Cas3ExternalReferralController(
  private val cas3AssessmentService: Cas3AssessmentService,
  private val cas3AssessmentTransformer: Cas3AssessmentTransformer,
) {

  @PreAuthorize("hasRole('ACCOMMODATION_API__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/referrals/{crn}")
  fun getReferralsByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<List<Cas3ReferralHistory>> = ResponseEntity.ok(
    cas3AssessmentService.getAssessmentsByCrn(crn).map {
      cas3AssessmentTransformer.transformAssessmentToCas3ReferralHistory(it)
    },
  )
}
