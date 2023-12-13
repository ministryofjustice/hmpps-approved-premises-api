package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

interface OffenderDetailsDataSource {
  val name: OffenderDetailsDataSourceName

  fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary>
  fun getOffenderDetailSummaries(crns: List<String>): List<ClientResult<OffenderDetailSummary>>
  fun getUserAccessForOffenderCrn(deliusUsername: String, crn: String): ClientResult<UserOffenderAccess>
  fun getUserAccessForOffenderCrns(deliusUsername: String, crns: List<String>): List<ClientResult<UserOffenderAccess>>
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

  override fun getOffenderDetailSummaries(crns: List<String>): List<ClientResult<OffenderDetailSummary>> =
    crns.map(this::getOffenderDetailSummary)

  override fun getUserAccessForOffenderCrn(deliusUsername: String, crn: String): ClientResult<UserOffenderAccess> =
    communityApiClient.getUserAccessForOffenderCrn(deliusUsername, crn)

  override fun getUserAccessForOffenderCrns(
    deliusUsername: String,
    crns: List<String>,
  ): List<ClientResult<UserOffenderAccess>> =
    crns.map { getUserAccessForOffenderCrn(deliusUsername, it) }
}

@Component
class ApDeliusContextApiOffenderDetailsDataSource : OffenderDetailsDataSource {
  override val name: OffenderDetailsDataSourceName
    get() = OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API

  override fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    throw NotImplementedError("Getting details for individual offenders from the AP Delius Context API is not currently supported")
  }

  override fun getOffenderDetailSummaries(crns: List<String>): List<ClientResult<OffenderDetailSummary>> {
    throw NotImplementedError("Getting details for multiple offenders from the AP Delius Context API is not currently supported")
  }

  override fun getUserAccessForOffenderCrn(
    deliusUsername: String,
    crn: String,
  ): ClientResult<UserOffenderAccess> {
    throw NotImplementedError("Getting user access for individual offenders from the AP Delius Context API is not currently supported")
  }

  override fun getUserAccessForOffenderCrns(
    deliusUsername: String,
    crns: List<String>,
  ): List<ClientResult<UserOffenderAccess>> {
    throw NotImplementedError("Getting user access for multiple offenders from the AP Delius Context API is not currently supported")
  }
}
