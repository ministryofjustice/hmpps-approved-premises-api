package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asUserOffenderAccess

interface OffenderDetailsDataSource {
  val name: OffenderDetailsDataSourceName

  fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary>
  fun getOffenderDetailSummaries(crns: List<String>): Map<String, ClientResult<OffenderDetailSummary>>
  fun getUserAccessForOffenderCrn(deliusUsername: String, crn: String): ClientResult<UserOffenderAccess>
  fun getUserAccessForOffenderCrns(deliusUsername: String, crns: List<String>): Map<String, ClientResult<UserOffenderAccess>>
}

enum class OffenderDetailsDataSourceName {
  COMMUNITY_API,
  AP_DELIUS_CONTEXT_API,
}

@Component
@Primary
class ConfiguredOffenderDetailsDataSource(
  dataSources: List<OffenderDetailsDataSource>,
  @Value("\${data-sources.offender-details}")
  private val dataSourceName: OffenderDetailsDataSourceName,
) : OffenderDetailsDataSource {
  private val log = LoggerFactory.getLogger(this::class.java)

  private lateinit var dataSource: OffenderDetailsDataSource

  init {
    log.info("Getting '$dataSourceName' offender details data source...")
    dataSource = dataSources.first { it.name == dataSourceName }
    log.info("Retrieved instance of type ${dataSource::class}")
  }

  override val name: OffenderDetailsDataSourceName
    get() = dataSource.name

  override fun getOffenderDetailSummary(crn: String) = dataSource.getOffenderDetailSummary(crn)
  override fun getOffenderDetailSummaries(crns: List<String>) = dataSource.getOffenderDetailSummaries(crns)
  override fun getUserAccessForOffenderCrn(deliusUsername: String, crn: String) = dataSource.getUserAccessForOffenderCrn(deliusUsername, crn)
  override fun getUserAccessForOffenderCrns(deliusUsername: String, crns: List<String>) = dataSource.getUserAccessForOffenderCrns(deliusUsername, crns)
}

@Component
class CommunityApiOffenderDetailsDataSource(
  private val communityApiClient: CommunityApiClient,
) : OffenderDetailsDataSource {
  override val name: OffenderDetailsDataSourceName
    get() = OffenderDetailsDataSourceName.COMMUNITY_API

  override fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    var offenderResponse = communityApiClient.getOffenderDetailSummaryWithWait(crn)

    if (offenderResponse is ClientResult.Failure.PreemptiveCacheTimeout) {
      offenderResponse = communityApiClient.getOffenderDetailSummaryWithCall(crn)
    }

    return offenderResponse
  }

  override fun getOffenderDetailSummaries(crns: List<String>): Map<String, ClientResult<OffenderDetailSummary>> =
    crns.associateBy(
      keySelector = { crn -> crn },
      valueTransform = { crn -> getOffenderDetailSummary(crn) },
    )

  override fun getUserAccessForOffenderCrn(deliusUsername: String, crn: String): ClientResult<UserOffenderAccess> =
    communityApiClient.getUserAccessForOffenderCrn(deliusUsername, crn)

  override fun getUserAccessForOffenderCrns(
    deliusUsername: String,
    crns: List<String>,
  ): Map<String, ClientResult<UserOffenderAccess>> =
    crns.associateBy(
      keySelector = { crn -> crn },
      valueTransform = { crn -> getUserAccessForOffenderCrn(deliusUsername, crn) },
    )
}

@Component
class ApDeliusContextApiOffenderDetailsDataSource(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) : OffenderDetailsDataSource {
  override val name: OffenderDetailsDataSourceName
    get() = OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API

  override fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    return getOffenderDetailSummaries(listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST") // Safe as we only do this for non-success types
  override fun getOffenderDetailSummaries(crns: List<String>): Map<String, ClientResult<OffenderDetailSummary>> {
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

  override fun getUserAccessForOffenderCrn(
    deliusUsername: String,
    crn: String,
  ): ClientResult<UserOffenderAccess> {
    return getUserAccessForOffenderCrns(deliusUsername, listOf(crn)).values.first()
  }

  @Suppress("UNCHECKED_CAST") // Safe as we only do this for non-success types
  override fun getUserAccessForOffenderCrns(
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
