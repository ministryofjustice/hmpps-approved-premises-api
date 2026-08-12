package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.CaseTiers
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persistence operations for Case entities used by the ensureCaseExists flow.
 *
 * Each operation executes within its own transaction to handle the
 * concurrent create/update workflow.
 *
 * In particular, create operations must commit or fail independently
 * so that unique constraint violations caused by concurrent inserts
 * can be handled by re-reading the case created by the competing request.
 *
 * The transaction boundaries in this class are intentional and should be
 * reviewed carefully before being changed.
 */
@Service
class CasePersistenceService(
  private val caseRepository: CaseRepository,
) {

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  fun getCase(crn: String) = caseRepository.findByCrn(crn)

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun createCase(caseSummary: CaseSummary, tiers: CaseTiers): CaseEntity {
    val now = OffsetDateTime.now()
    return CaseEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn.uppercase(),
      createdAt = now,
      lastUpdatedAt = now,
      name = caseSummary.buildName(),
      nomsNumber = caseSummary.nomsId,
      tierV2 = tiers.v2,
      tierV3 = tiers.v3,
    ).let(caseRepository::saveAndFlush)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun updateIfExist(
    crn: String,
    caseSummary: CaseSummary,
    tiers: CaseTiers,
  ): CaseEntity? {
    val entity = caseRepository.findByCrn(crn)
      ?: return null

    entity.name = caseSummary.buildName()
    entity.nomsNumber = caseSummary.nomsId
    if (tiers.v2 != null) {
      entity.tierV2 = tiers.v2
    }
    if (tiers.v3 != null) {
      entity.tierV3 = tiers.v3
    }

    caseRepository.saveAndFlush(entity)

    return entity
  }

  private fun CaseSummary.buildName() = "${name.forename} ${name.surname}".uppercase().trim()
}
