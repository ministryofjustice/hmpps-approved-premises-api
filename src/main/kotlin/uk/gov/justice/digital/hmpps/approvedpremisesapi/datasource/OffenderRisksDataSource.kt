package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks

interface OffenderRisksDataSource {
  val name: OffenderRisksDataSourceName

  fun getPersonRisks(crn: String): PersonRisks
}

enum class OffenderRisksDataSourceName {
  COMMUNITY_API,
  AP_DELIUS_CONTEXT_API,
}

@Component
@Primary
class ConfiguredOffenderRisksDataSource(
  dataSources: List<OffenderRisksDataSource>,
  @Value("\${data-sources.offender-risks}")
  private val dataSourceName: OffenderRisksDataSourceName,
) : OffenderRisksDataSource {
  private val log = LoggerFactory.getLogger(this::class.java)

  private lateinit var dataSource: OffenderRisksDataSource

  init {
    log.info("Getting '$dataSourceName' offender risks data source...")
    dataSource = dataSources.first { it.name == dataSourceName }
    log.info("Retrieved instance of type ${dataSource::class}")
  }

  override val name: OffenderRisksDataSourceName
    get() = dataSource.name

  override fun getPersonRisks(crn: String): PersonRisks =
    dataSource.getPersonRisks(crn)
}

@Component
class CommunityApiOffenderRisksDataSource : OffenderRisksDataSource {
  override val name: OffenderRisksDataSourceName
    get() = OffenderRisksDataSourceName.COMMUNITY_API

  override fun getPersonRisks(crn: String): PersonRisks {
    throw NotImplementedError("Getting risks for individual offenders from the Community API is not currently supported")
  }
}

@Component
class ApDeliusContextApiOffenderRisksDataSource : OffenderRisksDataSource {
  override val name: OffenderRisksDataSourceName
    get() = OffenderRisksDataSourceName.AP_DELIUS_CONTEXT_API

  override fun getPersonRisks(crn: String): PersonRisks {
    throw NotImplementedError("Getting risks for individual offenders from the AP Delius Context API is not currently supported")
  }
}
