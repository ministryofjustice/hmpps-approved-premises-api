package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1CreateApplicationLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas1Controller
@Tag(name = "CAS1 OASys")
class Cas1OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysOffenceDetailsTransformer: Cas1OASysOffenceDetailsTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @Operation(
    summary = "Returns metadata about supporting information questions for a given CRN",
    operationId = "metadata",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysMetadata::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/metadata"],
    produces = ["application/json"],
  )
  fun metadata(
    @PathVariable crn: String,
  ): ResponseEntity<Cas1OASysMetadata> {
    ensureOffenderAccess(crn)

    return ResponseEntity.ok(
      Cas1OASysMetadata(
        assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(
          extractNullableOAsysResult(oaSysService.getOffenceDetails(crn)),
        ),
        supportingInformation = cas1OASysNeedsQuestionTransformer.transformToSupportingInformationMetadata(
          extractNullableOAsysResult(oaSysService.getNeedsDetails(crn)),
        ),
      ),
    )
  }

  @Operation(
    summary = "Returns OASys answers for the requested group. The Supporting Information answers are returned if linked to harm and optionally if their section number appears in the selected-sections query parameter.",
    operationId = "answers",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysGroup::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/answers"],
    produces = ["application/json"],
  )
  fun answers(
    @PathVariable @Parameter(description = "CRN of the Person to fetch latest OASys selection") crn: String,
    @RequestParam group: Cas1OASysGroupName,
    @RequestParam includeOptionalSections: List<Int>?,
  ): ResponseEntity<Cas1OASysGroup> {
    ensureOffenderAccess(crn)

    val offenceDetails = extractNullableOAsysResult(oaSysService.getOffenceDetails(crn))

    val assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(offenceDetails)

    val answers = when (group) {
      Cas1OASysGroupName.RISK_MANAGEMENT_PLAN -> oaSysSectionsTransformer.riskManagementPlanAnswers(
        extractNullableOAsysResult(oaSysService.getRiskManagementPlan(crn))?.riskManagementPlan,
      )
      Cas1OASysGroupName.OFFENCE_DETAILS -> oaSysSectionsTransformer.offenceDetailsAnswers(offenceDetails?.offence)
      Cas1OASysGroupName.ROSH_SUMMARY -> oaSysSectionsTransformer.roshSummaryAnswers(
        extractNullableOAsysResult(oaSysService.getRoshSummary(crn))?.roshSummary,
      )
      Cas1OASysGroupName.SUPPORTING_INFORMATION -> cas1OASysNeedsQuestionTransformer.transformToOASysQuestion(
        needsDetails = extractNullableOAsysResult(oaSysService.getNeedsDetails(crn)),
        includeOptionalSections = includeOptionalSections ?: emptyList(),
        health = extractNullableOAsysResult(oaSysService.getHealthDetails(crn)),
      )
      Cas1OASysGroupName.RISK_TO_SELF -> oaSysSectionsTransformer.riskToSelfAnswers(
        extractNullableOAsysResult(oaSysService.getRiskToTheIndividual(crn))?.riskToTheIndividual,
      )
    }

    return ResponseEntity.ok(
      Cas1OASysGroup(
        group = group,
        assessmentMetadata = assessmentMetadata,
        answers = answers,
      ),
    )
  }

  @Operation(summary = "Returns OASys risk to the individual for a Person.")
  @GetMapping("/people/{crn}/oasys/risks-to-individual")
  fun getOasysRisksToIndividual(@PathVariable crn: String): ResponseEntity<RiskToTheIndividualInner> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_AP_RESIDENT_PROFILE)

    val risksToTheIndividualWrapper = extractEntityFromCasResult(
      oaSysService.getRiskToTheIndividual(crn),
    )

    val riskToTheIndividual = risksToTheIndividualWrapper.riskToTheIndividual
      ?: throw NotFoundProblem(crn, "RiskToTheIndividual")

    return ResponseEntity.ok(riskToTheIndividual)
  }

  @Operation(summary = "Returns OASys health details for a Person.")
  @GetMapping("/people/{crn}/oasys/health-details")
  fun getOasysHealthDetails(@PathVariable crn: String): ResponseEntity<HealthDetailsInner> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_AP_RESIDENT_PROFILE)

    val healthDetailsWrapper = extractEntityFromCasResult(
      oaSysService.getHealthDetails(crn),
    )

    val healthDetails = healthDetailsWrapper.health

    return ResponseEntity.ok(healthDetails)
  }

  @SuppressWarnings("ThrowsCount")
  private fun ensureOffenderAccess(crn: String) {
    when (
      offenderService.canAccessOffender(
        crn = crn,
        laoStrategy = userService.getUserForRequest().cas1CreateApplicationLaoStrategy(),
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
}
