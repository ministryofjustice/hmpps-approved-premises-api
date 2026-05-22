package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApAndOASysClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class OASysService(
  private val apAndOASysClient: ApAndOASysClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getAssessmentSummary(crn: String): CasResult<OASysAssessmentSummary> = when (val result = apAndOASysClient.getLatestAssessmentSummary(crn)) {
    is ClientResult.Success -> CasResult.Success(result.body)
    is ClientResult.Failure.StatusCode -> when (result.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("OASysAssessment", crn)
      else -> result.throwException()
    }
    is ClientResult.Failure -> result.throwException()
  }

  fun getOASysNeeds(crn: String): CasResult<NeedsDetails> = when (val needsResult = apAndOASysClient.getNeedsDetails(crn)) {
    is ClientResult.Success -> CasResult.Success(needsResult.body)
    is ClientResult.Failure.StatusCode -> when (needsResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> {
        log.warn("Failed to fetch OASys needs details - got response status ${needsResult.status}, returning 404")
        throw NotFoundProblem(crn, "OASys")
      }
    }
    is ClientResult.Failure.Other -> {
      log.warn("Failed to fetch OASys needs details, returning 404", needsResult.exception)
      throw NotFoundProblem(crn, "OASys")
    }
    is ClientResult.Failure -> needsResult.throwException()
  }

  fun getOASysOffenceDetails(crn: String): CasResult<OffenceDetails> = when (val offenceDetailsResult = apAndOASysClient.getOffenceDetails(crn)) {
    is ClientResult.Success -> CasResult.Success(offenceDetailsResult.body)
    is ClientResult.Failure.StatusCode -> when (offenceDetailsResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> offenceDetailsResult.throwException()
    }
    is ClientResult.Failure -> offenceDetailsResult.throwException()
  }

  fun getOASysRiskManagementPlan(crn: String): CasResult<RiskManagementPlan> = when (val riskManagementPlanResult = apAndOASysClient.getRiskManagementPlan(crn)) {
    is ClientResult.Success -> CasResult.Success(riskManagementPlanResult.body)
    is ClientResult.Failure.StatusCode -> when (riskManagementPlanResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> riskManagementPlanResult.throwException()
    }
    is ClientResult.Failure -> riskManagementPlanResult.throwException()
  }

  fun getOASysRoshSummary(crn: String): CasResult<RoshSummary> = when (val roshSummaryResult = apAndOASysClient.getRoshSummary(crn)) {
    is ClientResult.Success -> CasResult.Success(roshSummaryResult.body)
    is ClientResult.Failure.StatusCode -> when (roshSummaryResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> roshSummaryResult.throwException()
    }
    is ClientResult.Failure -> roshSummaryResult.throwException()
  }

  fun getOASysRiskToTheIndividual(crn: String): CasResult<RisksToTheIndividual> = when (val risksToTheIndividualResult = apAndOASysClient.getRiskToTheIndividual(crn)) {
    is ClientResult.Success -> CasResult.Success(risksToTheIndividualResult.body)
    is ClientResult.Failure.StatusCode -> when (risksToTheIndividualResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> risksToTheIndividualResult.throwException()
    }
    is ClientResult.Failure -> risksToTheIndividualResult.throwException()
  }

  fun getOASysHealthDetails(crn: String): CasResult<HealthDetails> = when (val healthDetailsResult = apAndOASysClient.getHealth(crn)) {
    is ClientResult.Success -> CasResult.Success(healthDetailsResult.body)
    is ClientResult.Failure.StatusCode -> when (healthDetailsResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> healthDetailsResult.throwException()
    }
    is ClientResult.Failure -> healthDetailsResult.throwException()
  }
}
