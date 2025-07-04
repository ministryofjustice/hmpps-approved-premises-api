package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@RestController
@RequestMapping(
  "\${api.base-path:}/cas3",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas3PeopleController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysOffenceDetailsTransformer: Cas3OASysOffenceDetailsTransformer,
) {

  @GetMapping("/people/{crn}/oasys/riskManagement")
  fun riskManagement(@PathVariable crn: String): ResponseEntity<Cas3OASysGroup> {
    ensureOffenderAccess(crn)

    val offenceDetails = extractEntityFromCasResult(oaSysService.getOASysOffenceDetails(crn))

    val assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(offenceDetails)

    val answers = oaSysSectionsTransformer.riskManagementPlanAnswers(
      extractEntityFromCasResult(oaSysService.getOASysRiskManagementPlan(crn)).riskManagementPlan,
    )

    return ResponseEntity.ok(
      Cas3OASysGroup(
        assessmentMetadata = assessmentMetadata,
        answers = answers,
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
      null -> throw NotFoundProblem(crn, "Offender")
      false -> throw ForbiddenProblem()
      else -> Unit
    }
  }
}
