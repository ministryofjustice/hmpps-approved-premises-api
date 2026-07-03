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
  ): CasResult<NeedsDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getNeedsDetails(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getOffenceDetails(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<OffenceDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getOffenceDetails(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getRiskManagementPlan(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RiskManagementPlan> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskManagementPlan(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getRoshSummary(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RoshSummary> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRoshSummary(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getRiskToTheIndividual(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RisksToTheIndividual> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRiskToTheIndividual(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getHealthDetails(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<HealthDetails> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getHealth(crn),
    suitabilityStrategy = suitabilityStrategy,
    toAssessmentDates = { it.toAssessmentDates(crn) },
  )

  fun getRoshRatings(
    crn: String,
    suitabilityStrategy: SuitabilityStrategy = SuitabilityStrategy.CompletedInLastSixMonths,
  ): CasResult<RoshRatings> = handleResponse(
    crn = crn,
    response = apAndOASysClient.getRoshRatings(crn),
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
