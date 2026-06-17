package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRiskToSelfDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRoshRatingsDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRoshSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2OASysTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas2Controller
class Cas2OASysController(
  val oasysService: OASysService,
  val cas2OASysTransformer: Cas2OASysTransformer,
) {

  @GetMapping("/people/{crn}/oasys/metadata")
  fun getOASysMetadataByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2OASysAssessmentMetadataDto> {
    val assessment = when (val result = oasysService.getAssessmentSummary(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2OASysTransformer.toOASysAssessmentMetadataDto(assessment))
  }

  @GetMapping("/people/{crn}/oasys/risk-to-self")
  fun getRiskToSelf(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2OAsysRiskToSelfDto> {
    val risksToTheIndividual = when (val result = oasysService.getRiskToTheIndividual(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2OASysTransformer.toOASysRiskToSelfDto(risksToTheIndividual))
  }

  @GetMapping("/people/{crn}/oasys/rosh-summary")
  fun getRoshSummary(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2OAsysRoshSummaryDto> {
    val roshSummary = when (val result = oasysService.getRoshSummary(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2OASysTransformer.toOASysRoshSummaryDto(roshSummary))
  }

  @GetMapping("/people/{crn}/oasys/rosh-ratings")
  fun getRoshRatings(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2OAsysRoshRatingsDto> {
    val roshRatings = when (val result = oasysService.getRoshRatings(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2OASysTransformer.toOASysRoshRatingsDto(roshRatings))
  }
}
