package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PremisesCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas1PremisesController(
  val userAccessService: UserAccessService,
  val cas1PremisesService: Cas1PremisesService,
  val cas1PremisesTransformer: Cas1PremisesTransformer,
) : PremisesCas1Delegate {

  override fun getPremisesById(premisesId: UUID): ResponseEntity<Cas1PremisesSummary> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PREMISES_VIEW_SUMMARY)

    return ResponseEntity
      .ok()
      .body(
        cas1PremisesTransformer.toPremiseSummary(
          extractEntityFromCasResult(cas1PremisesService.getPremisesSummary(premisesId)),
        ),
      )
  }
}
