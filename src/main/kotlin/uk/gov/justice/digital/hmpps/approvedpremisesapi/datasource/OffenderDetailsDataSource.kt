package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asUserOffenderAccess

@Component
class OffenderDetailsDataSource(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) {
  fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    return getOffenderDetailSummaries(listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST") // Safe as we only do this for non-success types
  fun getOffenderDetailSummaries(crns: List<String>): Map<String, ClientResult<OffenderDetailSummary>> {
    return when (val clientResult = apDeliusContextApiClient.getSummariesForCrns(crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.cases.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            if (access != null) {
              clientResult.copyWithBody(body = access.asOffenderDetailSummary())
            } else {
              ClientResult.Failure.StatusCode(
                method = HttpMethod.GET,
                path = "/probation-cases/summaries",
                status = HttpStatus.NOT_FOUND,
                body = null,
              )
            }
          },
        )
      }
      else -> return crns.associateWith { clientResult as ClientResult<OffenderDetailSummary> }
    }
  }

  fun getUserAccessForOffenderCrn(
    deliusUsername: String,
    crn: String,
  ): ClientResult<UserOffenderAccess> {
    return getUserAccessForOffenderCrns(deliusUsername, listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST") // Safe as we only do this for non-success types
  fun getUserAccessForOffenderCrns(
    deliusUsername: String,
    crns: List<String>,
  ): Map<String, ClientResult<UserOffenderAccess>> {
    return when (val clientResult = apDeliusContextApiClient.getUserAccessForCrns(deliusUsername, crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            if (access != null) {
              clientResult.copyWithBody(body = access.asUserOffenderAccess())
            } else {
              ClientResult.Failure.StatusCode(
                method = HttpMethod.GET,
                path = "/users/access?username=$deliusUsername",
                status = HttpStatus.NOT_FOUND,
                body = null,
              )
            }
          },
        )
      }
      else -> crns.associateWith { clientResult as ClientResult<UserOffenderAccess> }
    }
  }
}
