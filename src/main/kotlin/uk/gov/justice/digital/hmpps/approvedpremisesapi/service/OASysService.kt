package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApAndOASysClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo
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
  private val oasysApplicabilityService: OASysSuitabilityService,
) {
  fun getAssessmentSummary(crn: String): CasResult<OASysAssessmentSummary> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getLatestAssessmentSummary(crn),
    toApplicabilityInfo = {
      OASysSuitabilityService.OASysAssessmentDates(
        crn = crn,
        initiationDate = it.initiationDate,
        dateCompleted = it.completedDate,
      )
    },
  )

  fun getNeedsDetails(crn: String): CasResult<NeedsDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getNeedsDetails(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  fun getOffenceDetails(crn: String): CasResult<OffenceDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getOffenceDetails(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  fun getRiskManagementPlan(crn: String): CasResult<RiskManagementPlan> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskManagementPlan(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  fun getRoshSummary(crn: String): CasResult<RoshSummary> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRoshSummary(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  fun getRiskToTheIndividual(crn: String): CasResult<RisksToTheIndividual> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskToTheIndividual(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  fun getHealthDetails(crn: String): CasResult<HealthDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getHealth(crn),
    toApplicabilityInfo = { it.toApplicabilityInfo(crn) },
  )

  private fun <T> handleResponse(
    crn: String,
    response: ClientResult<T>,
    toApplicabilityInfo: (T) -> OASysSuitabilityService.OASysAssessmentDates,
  ) = when (response) {
    is ClientResult.Success -> {
      val body = response.body
      val applicabilityInfo = toApplicabilityInfo(body)
      if (oasysApplicabilityService.isUsable(applicabilityInfo)) {
        CasResult.Success(body)
      } else {
        notFound(crn)
      }
    }
    is ClientResult.Failure.StatusCode -> when (response.status) {
      HttpStatus.NOT_FOUND -> notFound(crn)
      else -> response.throwException()
    }
    is ClientResult.Failure -> response.throwException()
  }

  private fun AssessmentInfo.toApplicabilityInfo(crn: String) = OASysSuitabilityService.OASysAssessmentDates(
    crn = crn,
    initiationDate = initiationDate,
    dateCompleted = dateCompleted,
  )

  private fun <T> notFound(crn: String) = CasResult.NotFound<T>("OASysAssessment", crn)
}
