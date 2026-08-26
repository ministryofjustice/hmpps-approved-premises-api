package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.CaseTiers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.BackfillCasesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.transformer.toDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_USE_TIER_V3
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

/**
 * CAS maintains a local copy of NDelius Case data, allowing us to perform queries
 * across sets of that data and minimise upstream calls (e.g. sorting and searching
 * on tier and name)
 *
 * CAS only tracks cases that have been explicitly registered as 'of interest'. This
 * typically happens when an application is created via a call to
 * [ensureCaseExists]
 *
 * The tier values for a case are kept up-to-date by listening for tier update events
 *
 * The [BackfillCasesJob] migration job was used to seed this table originally, and can be
 * used if for some reason there are entries missing from this table (e.g. new CRNs were
 * introduced without a call to [ensureCaseExists])
 */
@Service
class CaseService(
  private val caseRepository: CaseRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val tierService: TierService,
  private val featureFlagService: FeatureFlagService,
  private val sentryService: SentryService,
  private val casePersistenceService: CasePersistenceService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    private const val CASES_CRN_UNIQUE_CONSTRAINT = "cas1_offenders_crn"
  }

  private val useTierV3: Boolean
    get() = featureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3)

  /**
   * Each persistence operation runs in its own transaction so concurrent
   * case creation can be handled safely. If another request inserts the
   * same case first, the database unique constraint rejects the duplicate
   * insert, after which the existing row is read and returned.
   */
  fun ensureCaseExists(crn: String): CaseDto {
    val normalizedCrn = crn.uppercase()
    val caseSummary = getCaseSummary(normalizedCrn)
    val tiers = fetchAvailableTiers(normalizedCrn)

    val existingCase = casePersistenceService.updateIfExist(normalizedCrn, caseSummary, tiers)
    if (existingCase != null) {
      return existingCase.toDto()
    } else {
      return try {
        casePersistenceService.createCase(caseSummary, tiers).toDto()
      } catch (e: DataIntegrityViolationException) {
        if (!isCrnDuplicateException(e)) {
          throw e
        }

        log.warn(
          "Failed to create case for CRN $normalizedCrn; " +
            "case may have been created concurrently.",
          e,
        )
        casePersistenceService.getCase(normalizedCrn)?.toDto() ?: throw e
      }
    }
  }

  fun reviseTier(crn: String): Boolean {
    val normalizedCrn = crn.uppercase()
    val case = caseRepository.findByCrn(normalizedCrn) ?: return false

    case.tierV2 = tierService.fetchTierOrError(normalizedCrn, TierVersion.V2)
    log.info("Have updated tierV2 for $normalizedCrn to $case.tierV2")

    case.tierV3 = tierService.fetchTierOrError(normalizedCrn, TierVersion.V3)
    log.info("Have updated tierV3 for $normalizedCrn to $case.tierV3")
    caseRepository.save(case)
    return true
  }

  /**
   * Get a case by CRN. Note that a case must have been previously registered via
   * a call to [ensureCaseExists] to be returned here. If a case isn't found an
   * alert will be raised so we can investigate this, as it should never happen
   */
  fun getCase(crn: String): CaseDto? {
    val normalizedCrn = crn.uppercase()
    val case = caseRepository.findByCrn(normalizedCrn)?.toDto()
    if (case == null) {
      alertCaseNotFound(normalizedCrn)
    }
    return case
  }

  private fun fetchAvailableTiers(crn: String) = CaseTiers(
    v2 = tierService.fetchTierOrNull(crn, TierVersion.V2),
    v3 = tierService.fetchTierOrNull(crn, TierVersion.V3),
  )

  private fun CaseEntity.toDto() = CaseDto(
    crn = crn,
    nomsNumber = nomsNumber,
    name = name,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    tier = if (useTierV3) {
      tierV3?.toDto()
    } else {
      tierV2?.toDto()
    },
  )

  private fun getCaseSummary(crn: String): CaseSummary = when (
    val caseSummariesResponse = apDeliusContextApiClient.getCaseSummaries(listOf(crn))
  ) {
    is ClientResult.Success ->
      caseSummariesResponse.body.cases.firstOrNull { it.crn == crn } ?: throw NotFoundProblem(crn, "Offender")

    is ClientResult.Failure -> caseSummariesResponse.throwException()
  }

  /**
   * This alert is a temporary measure whilst we introduce tiers across the
   * service. The intention is to help us detect if there are any routes in
   * the system where tier is required but a case does not yet exist for the CRN
   *
   * These paths may be legitimate (in which case a tier should not be requested)
   */
  private fun alertCaseNotFound(crn: String) {
    sentryService.captureException(CaseNotFound("Case with CRN $crn not found"))
  }

  private fun isCrnDuplicateException(
    e: DataIntegrityViolationException,
  ): Boolean = generateSequence<Throwable>(e) { it.cause }
    .filterIsInstance<ConstraintViolationException>()
    .any { it.constraintName == CASES_CRN_UNIQUE_CONSTRAINT }

  class CaseNotFound(message: String) : Exception(message)
}
