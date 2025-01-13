package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails

@Component
class Cas1BackfillOfflineApplicationName(
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val offenderService: OffenderService,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber")
  override fun process(pageSize: Int) {
    log.info("Starting migration process...")

    val applications = offlineApplicationRepository.findByNameIsNull()

    val personInfoResults = getPersonInfoResultsFromApplications(applications)

    val crnToNameMap = buildCrnToNameMap(personInfoResults)

    addNamesToApplications(applications, crnToNameMap)

    log.info("Offline application name update complete")
  }

  private fun getPersonInfoResultsFromApplications(applications: List<OfflineApplicationEntity>): List<PersonSummaryInfoResult> = offenderService.getPersonSummaryInfoResultsInBatches(
    applications.map { it.crn }.toSet(),
    OffenderService.LimitedAccessStrategy.IgnoreLimitedAccess,
  )

  private fun buildCrnToNameMap(personInfoResults: List<PersonSummaryInfoResult>): Map<String, String?> = personInfoResults.associate {
    it.crn to it.tryGetDetails { listOf(it.name.forename, it.name.surname).joinToString(" ") }
  }

  private fun addNamesToApplications(applications: List<OfflineApplicationEntity>, crnToNameMap: Map<String, String?>) {
    applications.forEach {
      updateApplicationName(it, crnToNameMap[it.crn])
    }
  }

  private fun updateApplicationName(application: OfflineApplicationEntity, name: String?) {
    if (name == null) {
      log.warn("No name found for offender associated with offline application ${application.id}")
    } else {
      application.name = name
      offlineApplicationRepository.save(application)
      log.info("Name of offender added to offline application ${application.id}")
    }
  }
}
