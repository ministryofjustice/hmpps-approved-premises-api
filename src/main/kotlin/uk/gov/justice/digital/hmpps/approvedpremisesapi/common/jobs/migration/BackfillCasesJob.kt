package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.BackfillCaseSummaryMigrationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.OffsetDateTime
import java.util.UUID

@Component
class BackfillCasesJob(
  private val caseRepository: CaseRepository,
  private val offenderService: OffenderService,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override val shouldRunInTransaction = false

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    val missingCaseDtos = caseRepository.findAllUniqueCrnsMissingFromCases()

    migrationLogger.info("Found ${missingCaseDtos.size} unique CRNs to backfill.")

    processInBatches(missingCaseDtos, pageSize) { batch ->
      val summariesByCrn = try {
        offenderService.getPersonSummaryInfoResultsInBatches(
          batch.map { it.crn }.toSet(),
          LaoStrategy.NeverRestricted,
        ).associateBy { it.crn }
      } catch (exception: Exception) {
        log.error(
          "Unable to retrieve person summaries for batch, continuing with DTO values",
          exception,
        )
        emptyMap()
      }

      batch.forEach { dto ->
        try {
          backfillCase(dto, summariesByCrn[dto.crn])
        } catch (exception: Exception) {
          log.error("Unable to backfill case for CRN ${dto.crn}", exception)
        }
      }
    }
  }

  private fun backfillCase(
    dto: BackfillCaseSummaryMigrationDto,
    summaryResult: PersonSummaryInfoResult?,
  ) {
    val summaryName: String?
    val summaryNomsNumber: String?

    when (summaryResult) {
      is PersonSummaryInfoResult.Success.Full -> {
        summaryName =
          "${summaryResult.summary.name.forename} ${summaryResult.summary.name.surname}"
            .uppercase()
        summaryNomsNumber = summaryResult.summary.nomsId
      }

      is PersonSummaryInfoResult.Success.Restricted -> {
        throw MigrationException("CRN ${dto.crn} is restricted")
      }
      else -> {
        summaryName = null
        summaryNomsNumber = null
      }
    }

    val name = summaryName ?: dto.name?.uppercase()
    val nomsNumber = summaryNomsNumber ?: dto.nomsNumber

    if (name == null) {
      log.error("Could not find name for CRN ${dto.crn} even with fallbacks. Skipping.")
      return
    }

    val now = OffsetDateTime.now()

    caseRepository.save(
      CaseEntity(
        id = UUID.randomUUID(),
        crn = dto.crn,
        name = name,
        nomsNumber = nomsNumber,
        tier = retrieveTierForCrn(dto),
        createdAt = now,
        lastUpdatedAt = now,
      ),
    )
  }

  private fun BackfillCasesJob.retrieveTierForCrn(dto: BackfillCaseSummaryMigrationDto): String? = when (val tierResponse = hmppsTierApiClient.getTier(dto.crn)) {
    is ClientResult.Success -> tierResponse.body.tierScore
    is ClientResult.Failure -> {
      log.warn("Could not retrieve tier for CRN ${dto.crn}")
      null
    }
  }
}
