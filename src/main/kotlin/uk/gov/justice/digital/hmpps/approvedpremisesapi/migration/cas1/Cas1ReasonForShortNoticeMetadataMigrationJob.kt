package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob

class Cas1ReasonForShortNoticeMetadataMigrationJob(
  private val applicationRepository: ApplicationRepository,
  private val domainEventRepository: DomainEventRepository,
  private val transactionTemplate: TransactionTemplate,
  private val jdbcTemplate: JdbcTemplate,
) : MigrationJob() {

  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  override fun process() {
    log.info("Starting Reason for Short Notice Migration process...")

    val jsonPathConfig = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build()

    val applicationIds = applicationRepository.findAllSubmittedApprovedPremisesApplicationsWithShortNotice()

    log.info("There are ${applicationIds.size} applications to update")

    for (applicationId in applicationIds) {
      log.debug("Updating $applicationId")

      transactionTemplate.executeWithoutResult {
        val application = applicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

        val json = JsonPath.parse(application.data, jsonPathConfig)
        val reasonForShortNotice = json.read<String>("basic-information.reason-for-short-notice.reason")
        val reasonForShortNoticeOther = json.read<String>("basic-information.reason-for-short-notice.other")
        val submittedEvent = domainEventRepository.getByApplicationIdAndType(
          applicationId = applicationId,
          type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
        )

        insertMetadataIfNotEmpty(submittedEvent, MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE, reasonForShortNotice)
        insertMetadataIfNotEmpty(submittedEvent, MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER, reasonForShortNoticeOther)
      }
    }

    log.info("Short Notice Migration process complete")
  }

  private fun insertMetadataIfNotEmpty(
    domainEvent: DomainEventEntity,
    name: MetaDataName,
    value: String?,
  ) {
    if (value.isNullOrEmpty()) {
      log.debug("Not setting $name as provided value is null or empty")
      return
    }

    if (domainEvent.metadata.containsKey(name)) {
      log.debug("Not overwriting $name as a value already exists")
      return
    }

    jdbcTemplate.update(
      "INSERT into domain_events_metadata(domain_event_id,name,value) VALUES(?,?,?)",
      domainEvent.id,
      name.name,
      value,
    )
  }
}
