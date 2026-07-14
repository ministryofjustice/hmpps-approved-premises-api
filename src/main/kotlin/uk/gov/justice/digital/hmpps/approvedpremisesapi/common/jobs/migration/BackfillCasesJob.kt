package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.BackfillCaseSummaryMigrationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier as UpstreamTier

@Component
@SuppressWarnings("MaxLineLength")
class BackfillCasesJob(
  private val caseRepository: CaseRepository,
  private val offenderService: OffenderService,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val migrationLogger: MigrationLogger,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {

  override val shouldRunInTransaction = false

  @SuppressWarnings("TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    val caseDtos = caseRepository.findUniqueCrnsForBackfill()

    processInBatches(caseDtos, pageSize) { batch ->
      val missingCases = batch.filter { !it.caseExists }

      val missingCasesSummary = fetchSummariesForMissingCases(missingCases)

      batch.forEach { dto ->
        runCatching {
          transactionTemplate.executeWithoutResult {
            processCase(dto, missingCasesSummary[dto.crn])
          }
        }.onFailure {
          migrationLogger.error("Unable to process case for CRN ${dto.crn}", it)
        }
      }
    }
  }

  @SuppressWarnings("MagicNumber")
  private fun <T> processInBatches(
    items: List<T>,
    batchSize: Int,
    processBatch: (List<T>) -> Unit,
  ) {
    val chunkedItems = items.chunked(batchSize)
    chunkedItems.forEachIndexed { index, batch ->
      migrationLogger.info("Processing batch ${index + 1} of ${chunkedItems.size}...")
      processBatch(batch)

      if (index < chunkedItems.lastIndex) {
        Thread.sleep(500L)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun fetchSummariesForMissingCases(
    newCaseDtos: List<BackfillCaseSummaryMigrationDto>,
  ): Map<String, PersonSummaryInfoResult> {
    val crns = newCaseDtos.map { it.crn }.toSet()

    if (crns.isEmpty()) return emptyMap()

    return try {
      offenderService.getPersonSummaryInfoResultsInBatches(
        crns,
        LaoStrategy.NeverRestricted,
      ).associateBy { it.crn }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to retrieve person summaries for batch, continuing with DTO values", exception)
      emptyMap()
    }
  }

  private fun processCase(
    dto: BackfillCaseSummaryMigrationDto,
    summaryResult: PersonSummaryInfoResult?,
  ) {
    if (dto.caseExists) {
      updateExistingCase(dto)
    } else {
      createCase(dto, summaryResult)
    }
  }

  private fun updateExistingCase(dto: BackfillCaseSummaryMigrationDto) {
    if (dto.hasTierV2 && dto.hasTierV3) {
      return
    }

    migrationLogger.info("Updating missing tiers for CRN ${dto.crn}")

    val existingCase = caseRepository.findByCrn(dto.crn)!!

    existingCase.apply {
      if (!dto.hasTierV2) tierV2 = fetchTierOrNull(dto.crn, TierVersion.V2)
      if (!dto.hasTierV3) tierV3 = fetchTierOrNull(dto.crn, TierVersion.V3)
      lastUpdatedAt = OffsetDateTime.now()
    }

    caseRepository.save(existingCase)
  }

  private fun createCase(
    dto: BackfillCaseSummaryMigrationDto,
    summaryResult: PersonSummaryInfoResult?,
  ) {
    migrationLogger.info("Creating case for CRN ${dto.crn}")

    val personDetails = resolvePersonDetails(dto, summaryResult)

    val now = OffsetDateTime.now()

    caseRepository.save(
      CaseEntity(
        id = UUID.randomUUID(),
        crn = dto.crn,
        name = personDetails.name,
        nomsNumber = personDetails.nomsNumber,
        tierV2 = fetchTierOrNull(dto.crn, TierVersion.V2),
        tierV3 = fetchTierOrNull(dto.crn, TierVersion.V3),
        createdAt = now,
        lastUpdatedAt = now,
      ),
    )
  }

  private fun resolvePersonDetails(
    dto: BackfillCaseSummaryMigrationDto,
    summaryResult: PersonSummaryInfoResult?,
  ): PersonDetails = when (summaryResult) {
    is PersonSummaryInfoResult.Success.Full ->
      PersonDetails(
        name = "${summaryResult.summary.name.forename} ${summaryResult.summary.name.surname}"
          .uppercase(),
        nomsNumber = summaryResult.summary.nomsId,
      )
    else ->
      PersonDetails(
        name = dto.name?.uppercase(),
        nomsNumber = dto.nomsNumber,
      )
  }

  private fun fetchTierOrNull(
    crn: String,
    version: TierVersion,
  ): Tier? = when (val response = hmppsTierApiClient.getTier(crn, version)) {
    is ClientResult.Success -> response.body.toTier(version)
    is ClientResult.Failure -> null
  }

  private fun UpstreamTier.toTier(version: TierVersion) = Tier(
    tierScore = tierScore,
    calculationId = calculationId,
    calculationDate = calculationDate,
    changeReason = changeReason,
    provisional = provisional,
    version = version,
  )

  private data class PersonDetails(
    val name: String?,
    val nomsNumber: String?,
  )
}
