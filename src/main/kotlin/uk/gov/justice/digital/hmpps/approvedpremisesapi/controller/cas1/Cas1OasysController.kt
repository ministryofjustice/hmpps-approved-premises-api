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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OASysAssessmentSuitabilityStrategyDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService.SuitabilityStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1CreateApplicationLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas1Controller
@Tag(name = "CAS1 OASys")
class Cas1OasysController(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer,
  private val oaSysService: OASysService,
  private val cas1OASysAssessmentInfoTransformer: Cas1OASysAssessmentInfoTransformer,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @Operation(
    summary = "Returns metadata about supporting information questions for a given CRN,",
    operationId = "metadata",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysMetadata::class))]),
      ApiResponse(responseCode = "404", description = "An OASys assessment completed in the last 6 months doesn't exist", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/metadata"],
    produces = ["application/json"],
  )
  fun metadata(
    @PathVariable crn: String,
    @RequestParam(defaultValue = "completed_in_last_six_months", name = "suitabilityStrategy") suitabilityStrategyDto: Cas1OASysAssessmentSuitabilityStrategyDto,
  ): ResponseEntity<Cas1OASysMetadata> {
    ensureOffenderAccess(crn)

    val needDetails = extractNullableOAsysResult(oaSysService.getNeedsDetails(crn, suitabilityStrategyDto.toSuitabilityStrategy()))

    return ResponseEntity.ok(
      Cas1OASysMetadata(
        assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(needDetails),
        supportingInformation = cas1OASysNeedsQuestionTransformer.transformToSupportingInformationMetadata(needDetails),
      ),
    )
  }

  @Operation(
    summary = "Returns OASys answers for the requested group, if the most recent OAsys Assessment was completed in the last 6 months. " +
      "The Supporting Information answers are returned if linked to harm and optionally if their section number appears in the selected-sections query parameter.",
    operationId = "answers",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysGroup::class))]),
      ApiResponse(responseCode = "404", description = "An OASys assessment completed in the last 6 months doesn't exist", content = [Content(schema = Schema(implementation = Problem::class))]),
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
    @RequestParam(defaultValue = "completed_in_last_six_months", name = "suitabilityStrategy") suitabilityStrategyDto: Cas1OASysAssessmentSuitabilityStrategyDto,
  ): ResponseEntity<Cas1OASysGroup> {
    ensureOffenderAccess(crn)

    val suitabilityStrategy = suitabilityStrategyDto.toSuitabilityStrategy()

    val group = when (group) {
      Cas1OASysGroupName.RISK_MANAGEMENT_PLAN -> {
        val riskManagementPlan = extractNullableOAsysResult(oaSysService.getRiskManagementPlan(crn, suitabilityStrategy))
        Cas1OASysGroup(
          group = group,
          assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(riskManagementPlan),
          answers = oaSysSectionsTransformer.riskManagementPlanAnswers(riskManagementPlan?.riskManagementPlan),
        )
      }
      Cas1OASysGroupName.OFFENCE_DETAILS -> {
        val offenceDetails = extractNullableOAsysResult(oaSysService.getOffenceDetails(crn, suitabilityStrategy))
        Cas1OASysGroup(
          group = group,
          assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(offenceDetails),
          answers = oaSysSectionsTransformer.offenceDetailsAnswers(offenceDetails?.offence),
        )
      }
      Cas1OASysGroupName.ROSH_SUMMARY -> {
        val roshSummary = extractNullableOAsysResult(oaSysService.getRoshSummary(crn, suitabilityStrategy))
        Cas1OASysGroup(
          group = group,
          assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(roshSummary),
          answers = oaSysSectionsTransformer.roshSummaryAnswers(roshSummary?.roshSummary),
        )
      }
      Cas1OASysGroupName.SUPPORTING_INFORMATION -> {
        val needsDetails = extractNullableOAsysResult(oaSysService.getNeedsDetails(crn, suitabilityStrategy))
        val healthDetails = extractNullableOAsysResult(oaSysService.getHealthDetails(crn, suitabilityStrategy))
        Cas1OASysGroup(
          group = group,
          assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(needsDetails),
          answers = cas1OASysNeedsQuestionTransformer.transformToOASysQuestions(
            needsDetails = needsDetails,
            sectionsToInclude = includeOptionalSections ?: emptyList(),
            healthDetails = healthDetails,
          ),
        )
      }
      Cas1OASysGroupName.RISK_TO_SELF -> {
        val riskToTheIndividual = extractNullableOAsysResult(oaSysService.getRiskToTheIndividual(crn, suitabilityStrategy))
        Cas1OASysGroup(
          group = group,
          assessmentMetadata = cas1OASysAssessmentInfoTransformer.toAssessmentMetadata(riskToTheIndividual),
          answers = oaSysSectionsTransformer.riskToSelfAnswers(riskToTheIndividual?.riskToTheIndividual),
        )
      }
    }

    return ResponseEntity.ok(group)
  }

  @Operation(
    summary = "Returns OASys risk to the individual for a Person. This will be taken from the most recently completed OASys assessment, regardless of its age",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysGroup::class))]),
      ApiResponse(responseCode = "404", description = "A completed OASys assessment doesn't exist", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @GetMapping("/people/{crn}/oasys/risks-to-individual")
  fun getOasysRisksToIndividual(@PathVariable crn: String): ResponseEntity<RiskToTheIndividualInner> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_AP_RESIDENT_PROFILE)

    val risksToTheIndividualWrapper = extractEntityFromCasResult(
      oaSysService.getRiskToTheIndividual(
        crn = crn,
        suitabilityStrategy = SuitabilityStrategy.AllowAll,
      ),
    )

    val riskToTheIndividual = risksToTheIndividualWrapper.riskToTheIndividual

    return ResponseEntity.ok(riskToTheIndividual)
  }

  @Operation(
    summary = "Returns OASys health details for a Person. This will be taken from the most recently completed OASys assessment, regardless of its age",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysGroup::class))]),
      ApiResponse(responseCode = "404", description = "A completed OASys assessment doesn't exist", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @GetMapping("/people/{crn}/oasys/health-details")
  fun getOasysHealthDetails(@PathVariable crn: String): ResponseEntity<HealthDetailsInner> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_AP_RESIDENT_PROFILE)

    val healthDetailsWrapper = extractEntityFromCasResult(
      oaSysService.getHealthDetails(
        crn = crn,
        suitabilityStrategy = SuitabilityStrategy.AllowAll,
      ),
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

  private fun Cas1OASysAssessmentSuitabilityStrategyDto.toSuitabilityStrategy() = when (this) {
    Cas1OASysAssessmentSuitabilityStrategyDto.ALLOW_ALL -> SuitabilityStrategy.AllowAll
    Cas1OASysAssessmentSuitabilityStrategyDto.COMPLETED_IN_LAST_SIX_MONTHS -> SuitabilityStrategy.CompletedInLastSixMonths
  }
}
