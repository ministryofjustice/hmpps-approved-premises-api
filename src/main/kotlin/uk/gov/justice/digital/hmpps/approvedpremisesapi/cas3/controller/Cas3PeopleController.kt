package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas3Controller
class Cas3PeopleController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysAssessmentInfoTransformer: Cas3OASysAssessmentInfoTransformer,
) {

  @GetMapping("/people/{crn}/oasys/riskManagement")
  fun riskManagement(@PathVariable crn: String): ResponseEntity<Cas3OASysGroup> {
    ensureOffenderAccess(crn)

    val riskManagementPlan = extractEntityFromCasResult(oaSysService.getRiskManagementPlan(crn))

    return ResponseEntity.ok(
      Cas3OASysGroup(
        assessmentMetadata = oaSysAssessmentInfoTransformer.toAssessmentMetadata(riskManagementPlan),
        answers = oaSysSectionsTransformer.riskManagementPlanAnswers(riskManagementPlan.riskManagementPlan),
      ),
    )
  }

  @SuppressWarnings("ThrowsCount")
  private fun ensureOffenderAccess(crn: String) {
    when (
      offenderService.canAccessOffender(
        crn = crn,
        laoStrategy = userService.getUserForRequest().cas3LaoStrategy(),
      )
    ) {
      false -> throw ForbiddenProblem()
      else -> Unit
    }
  }
}
