package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3OASysAssessmentSuitabilityStrategyDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService.SuitabilityStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas3Controller
class Cas3OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysAssessmentInfoTransformer: Cas3OASysAssessmentInfoTransformer,
) {

  @GetMapping("/people/{crn}/oasys/riskManagement")
  fun riskManagement(
    @PathVariable crn: String,
    @RequestParam(required = false, defaultValue = "completed_in_last_six_months", name = "suitabilityStrategy") suitabilityStrategyDto: Cas3OASysAssessmentSuitabilityStrategyDto?,
  ): ResponseEntity<Cas3OASysGroup> {
    ensureOffenderAccess(crn)

    val suitabilityStrategy = (suitabilityStrategyDto ?: Cas3OASysAssessmentSuitabilityStrategyDto.COMPLETED_IN_LAST_SIX_MONTHS).toSuitabilityStrategy()

    val riskManagementPlan = extractNullableOAsysResult(oaSysService.getRiskManagementPlan(crn, suitabilityStrategy))

    return ResponseEntity.ok(
      Cas3OASysGroup(
        assessmentMetadata = oaSysAssessmentInfoTransformer.toAssessmentMetadata(riskManagementPlan),
        answers = oaSysSectionsTransformer.riskManagementPlanAnswers(riskManagementPlan?.riskManagementPlan),
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

  private fun <EntityType> extractNullableOAsysResult(result: CasResult<EntityType>) = when (result) {
    is CasResult.NotFound -> null
    else -> extractEntityFromCasResult(result)
  }

  private fun Cas3OASysAssessmentSuitabilityStrategyDto.toSuitabilityStrategy() = when (this) {
    Cas3OASysAssessmentSuitabilityStrategyDto.ALLOW_ALL -> SuitabilityStrategy.AllowAll
    Cas3OASysAssessmentSuitabilityStrategyDto.COMPLETED_IN_LAST_SIX_MONTHS -> SuitabilityStrategy.CompletedInLastSixMonths
  }
}
