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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService.SuitabilityStrategy

@Service
class OASysService(
  private val apAndOASysClient: ApAndOASysClient,
  private val oasysSuitabilityService: OASysSuitabilityService,
) {
  fun getAssessmentSummary(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<OASysAssessmentSummary> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getLatestAssessmentSummary(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = {
      OASysSuitabilityService.OASysAssessmentDates(
        crn = crn,
        initiationDate = it.initiationDate,
        dateCompleted = it.completedDate,
      )
    },
  )

  fun getNeedsDetails(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<NeedsDetails> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getNeedsDetails(crn))

  fun getOffenceDetails(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<OffenceDetails> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getOffenceDetails(crn))

  fun getRiskManagementPlan(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RiskManagementPlan> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getRiskManagementPlan(crn))

  fun getRoshSummary(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RoshSummary> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getRoshSummary(crn))

  fun getRiskToTheIndividual(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RisksToTheIndividual> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getRiskToTheIndividual(crn))

  fun getHealthDetails(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<HealthDetails> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getHealth(crn))

  fun getRoshRatings(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RoshRatings> = handleAssessmentInfoResponse(crn, suitabilityStrategy, apAndOASysClient.getRoshRatings(crn))

  private fun <T : AssessmentInfo> handleAssessmentInfoResponse(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy,
    response: ClientResult<T>,
  ): CasResult<T> = handleResponse(
    crn = crn,
    response = response,
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  private fun <T> handleResponse(
    crn: String,
    response: ClientResult<T>,
    suitabilityStrategy: SuitabilityStrategy,
    toAssessmentDates: (T) -> OASysSuitabilityService.OASysAssessmentDates,
  ) = when (response) {
    is ClientResult.Success -> {
      val body = response.body
      if (oasysSuitabilityService.isSuitable(
          assessmentDates = toAssessmentDates(body),
          strategy = suitabilityStrategy,
        )
      ) {
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

  private fun <T> notFound(crn: String) = CasResult.NotFound<T>("OASysAssessment", crn)
}
