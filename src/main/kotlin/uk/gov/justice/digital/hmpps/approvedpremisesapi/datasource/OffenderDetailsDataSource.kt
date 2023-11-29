package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary

interface OffenderDetailsDataSource {
  val name: OffenderDetailsDataSourceName

  fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary>
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
}

@Component
class CommunityApiOffenderDetailsDataSource : OffenderDetailsDataSource {
  override val name: OffenderDetailsDataSourceName
    get() = OffenderDetailsDataSourceName.COMMUNITY_API

  override fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    throw NotImplementedError("Getting details for individual offenders from the Community API is not currently supported")
  }
}

@Component
class ApDeliusContextApiOffenderDetailsDataSource : OffenderDetailsDataSource {
  override val name: OffenderDetailsDataSourceName
    get() = OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API

  override fun getOffenderDetailSummary(crn: String): ClientResult<OffenderDetailSummary> {
    throw NotImplementedError("Getting details for individual offenders from the AP Delius Context API is not currently supported")
  }
}
