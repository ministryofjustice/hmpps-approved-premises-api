package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

@Service
class OASysService(
  private val apAndOASysClient: ApAndOASysClient,
) {
  fun getAssessmentSummary(crn: String): CasResult<OASysAssessmentSummary> = when (val result = apAndOASysClient.getLatestAssessmentSummary(crn)) {
    is ClientResult.Success -> CasResult.Success(result.body)
    is ClientResult.Failure.StatusCode -> when (result.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("OASysAssessment", crn)
      else -> result.throwException()
    }
    is ClientResult.Failure -> result.throwException()
  }

  fun getNeedsDetails(crn: String): CasResult<NeedsDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getNeedsDetails(crn),
  )

  fun getOffenceDetails(crn: String): CasResult<OffenceDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getOffenceDetails(crn),
  )

  fun getRiskManagementPlan(crn: String): CasResult<RiskManagementPlan> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskManagementPlan(crn),
  )

  fun getRoshSummary(crn: String): CasResult<RoshSummary> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRoshSummary(crn),
  )

  fun getRiskToTheIndividual(crn: String): CasResult<RisksToTheIndividual> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskToTheIndividual(crn),
  )

  fun getHealthDetails(crn: String): CasResult<HealthDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getHealth(crn),
  )

  private fun <T> handleResponse(crn: String, response: ClientResult<T>) = when (response) {
    is ClientResult.Success -> CasResult.Success(response.body)
    is ClientResult.Failure.StatusCode -> when (response.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("OASysAssessment", crn)
      else -> response.throwException()
    }
    is ClientResult.Failure -> response.throwException()
  }
}
