package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jobs.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2IdApplicationOriginEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJob

@Component
class Cas2HdcBackfillApplicationCohortJob(
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  override val shouldRunInTransaction = false
  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("MagicNumber")
  override fun process(pageSize: Int) {
    val applications = cas2ApplicationRepository.findIdAndApplicationOriginByCohortIsNull()

    log.info("Found ${applications.size} applications without a cohort to backfill")

    applications.forEach { application ->
      transactionTemplate.executeWithoutResult {
        updateCohort(application)
      }
      Thread.sleep(50)
    }

    log.info("Backfilled ${applications.size} applications")
  }

  private fun updateCohort(application: Cas2IdApplicationOriginEntity) {
    cas2ApplicationRepository.updateCohort(
      application.id,
      when (application.applicationOrigin) {
        ApplicationOrigin.courtBail -> Cas2Cohort.COURT_BAIL.name
        ApplicationOrigin.prisonBail -> Cas2Cohort.PRISON_BAIL.name
        ApplicationOrigin.homeDetentionCurfew -> Cas2Cohort.HDC.name
        ApplicationOrigin.other -> throw IllegalStateException("Unexpected application origin: ${application.applicationOrigin}")
      },
    )
  }
}
