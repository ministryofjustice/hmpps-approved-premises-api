package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.tryGetDetails
import java.lang.Boolean.FALSE
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.chunked

@Component
class Cas1UpdateApprovedPremisesApplicationWithOffenderJob(
  private val offenderService: OffenderService,
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val offenderRisksService: OffenderRisksService,
  private val migrationLogger: MigrationLogger,
  private val transactionTemplate: TransactionTemplate,
  private val cas1OffenderService: Cas1OffenderService,
  override val shouldRunInTransaction: Boolean = FALSE,
) : MigrationJob() {

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting migration process...")
    val applicationIds = getAllApplicationIdsWithoutOffender()
    processInBatches(applicationIds) { batchIds ->
      processApplicationsBatch(batchIds)
    }
    migrationLogger.info("Completed migration process...")
  }

  private fun getAllApplicationIdsWithoutOffender(): List<UUID> = approvedPremisesApplicationRepository.findAllIdsByCas1OffenderEntityIsNull()

  @SuppressWarnings("MagicNumber")
  private fun <T> processInBatches(
    items: List<T>,
    batchSize: Int = 500,
    processBatch: (List<T>) -> Unit,
  ) {
    items.chunked(batchSize).forEachIndexed { index, batch ->
      migrationLogger.info("Processing batch ${index + 1} of ${items.size / batchSize}...")
      transactionTemplate.executeWithoutResult {
        processBatch(batch)
      }
      if (index < items.chunked(batchSize).lastIndex) {
        Thread.sleep(1000L)
      }
    }
  }

  private fun processApplicationsBatch(applicationIds: List<UUID>) {
    val applicationsBatch = approvedPremisesApplicationRepository.findByIdIn(applicationIds)
    val crnOffenderMap = getOffenders(applicationsBatch)
    updateApplicationsWithOffenders(applicationsBatch, crnOffenderMap)
    migrationLogger.info("Processed ${applicationsBatch.size} applications.")
  }

  private fun getOffenders(applications: List<ApprovedPremisesApplicationEntity>): Map<String, Cas1OffenderEntity> = fetchPersonInfoResults(applications)
    .map { personResult ->
      val crn = personResult.crn
      val risks = offenderRisksService.getPersonRisks(crn)

      val caseSummary = personResult.tryGetDetails { it }

      if (caseSummary != null) {
        val offenderEntity = cas1OffenderService.getOrCreateOffender(caseSummary, risks)
        crn to offenderEntity
      } else { // create offender using application details
        val application = applications.find { it.crn == crn }!!
        val caseSummary = buildCaseSummaryFromApplication(application)
        val offenderEntity = cas1OffenderService.getOrCreateOffender(caseSummary, risks)
        crn to offenderEntity
      }
    }
    .toMap()

  private fun fetchPersonInfoResults(applications: List<ApprovedPremisesApplicationEntity>): List<PersonSummaryInfoResult> {
    val crnSet = applications.map { it.crn }.toSet()
    return offenderService.getPersonSummaryInfoResultsInBatches(crnSet, LaoStrategy.NeverRestricted)
  }

  private fun updateApplicationsWithOffenders(
    applications: List<ApprovedPremisesApplicationEntity>,
    crnOffenderMap: Map<String, Cas1OffenderEntity>,
  ) {
    applications.forEach { application ->
      crnOffenderMap[application.crn]?.let { offender ->
        application.cas1OffenderEntity = offender
      }
    }
    approvedPremisesApplicationRepository.saveAllAndFlush(applications)
  }
}

private fun buildCaseSummaryFromApplication(
  application: ApprovedPremisesApplicationEntity,
): CaseSummary {
  val caseSummary = CaseSummary(
    crn = application.crn,
    nomsId = application.nomsNumber,
    pnc = null,
    name = Name(
      forename = application.name,
      surname = "",
      middleNames = emptyList(),
    ),
    dateOfBirth = LocalDate.now(),
    gender = null,
    profile = null,
    manager = Manager(
      team = Team(
        code = "",
        name = "",
        ldu = Ldu(
          code = "",
          name = "",
        ),
        borough = null,
        startDate = null,
        endDate = null,
      ),
    ),
    currentExclusion = false,
    currentRestriction = false,
  )
  return caseSummary
}
