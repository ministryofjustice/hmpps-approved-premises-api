package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OAsysRiskToSelfDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2OASysTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas2v2Controller
class Cas2V2OASysController(
  val oasysService: OASysService,
  val cas2v2OASysTransformer: Cas2v2OASysTransformer,
) {

  @GetMapping("/people/{crn}/oasys/metadata")
  fun getOASysMetadataByCrn(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2v2OASysAssessmentMetadataDto> {
    val assessment = when (val result = oasysService.getAssessmentSummary(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2v2OASysTransformer.toOASysAssessmentMetadataDto(assessment))
  }

  @GetMapping("/people/{crn}/oasys/risk-to-self")
  fun getRiskToSelf(
    @PathVariable crn: String,
  ): ResponseEntity<Cas2v2OAsysRiskToSelfDto> {
    val risksToTheIndividual = when (val result = oasysService.getRiskToTheIndividual(crn)) {
      is CasResult.NotFound -> null
      else -> extractEntityFromCasResult(result)
    }

    return ResponseEntity.ok(cas2v2OASysTransformer.toOASysRiskToSelfDto(risksToTheIndividual))
  }
}
