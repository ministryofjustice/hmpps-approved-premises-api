package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class OASysService(
  private val apOASysContextApiClient: ApOASysContextApiClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getOASysNeeds(crn: String): CasResult<NeedsDetails> = when (val needsResult = apOASysContextApiClient.getNeedsDetails(crn)) {
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

  fun getOASysOffenceDetails(crn: String): CasResult<OffenceDetails> = when (val offenceDetailsResult = apOASysContextApiClient.getOffenceDetails(crn)) {
    is ClientResult.Success -> CasResult.Success(offenceDetailsResult.body)
    is ClientResult.Failure.StatusCode -> when (offenceDetailsResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> offenceDetailsResult.throwException()
    }
    is ClientResult.Failure -> offenceDetailsResult.throwException()
  }

  fun getOASysRiskManagementPlan(crn: String): CasResult<RiskManagementPlan> = when (val riskManagementPlanResult = apOASysContextApiClient.getRiskManagementPlan(crn)) {
    is ClientResult.Success -> CasResult.Success(riskManagementPlanResult.body)
    is ClientResult.Failure.StatusCode -> when (riskManagementPlanResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> riskManagementPlanResult.throwException()
    }
    is ClientResult.Failure -> riskManagementPlanResult.throwException()
  }

  fun getOASysRoshSummary(crn: String): CasResult<RoshSummary> = when (val roshSummaryResult = apOASysContextApiClient.getRoshSummary(crn)) {
    is ClientResult.Success -> CasResult.Success(roshSummaryResult.body)
    is ClientResult.Failure.StatusCode -> when (roshSummaryResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> roshSummaryResult.throwException()
    }
    is ClientResult.Failure -> roshSummaryResult.throwException()
  }

  fun getOASysRiskToTheIndividual(crn: String): CasResult<RisksToTheIndividual> = when (val risksToTheIndividualResult = apOASysContextApiClient.getRiskToTheIndividual(crn)) {
    is ClientResult.Success -> CasResult.Success(risksToTheIndividualResult.body)
    is ClientResult.Failure.StatusCode -> when (risksToTheIndividualResult.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Person", crn)
      HttpStatus.FORBIDDEN -> CasResult.Unauthorised()
      else -> risksToTheIndividualResult.throwException()
    }
    is ClientResult.Failure -> risksToTheIndividualResult.throwException()
  }
}
