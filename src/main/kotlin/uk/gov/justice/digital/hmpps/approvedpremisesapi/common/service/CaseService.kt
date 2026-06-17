package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CaseService(
  private val caseRepository: CaseRepository,
) {

  fun ensureCaseExists(caseSummary: CaseSummary, riskRatings: PersonRisks?): CaseEntity {
    val caseEntity = caseRepository.findByCrn(caseSummary.crn)?.apply {
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim()
      tier = riskRatings?.tier?.value?.level
      nomsNumber = caseSummary.nomsId
    } ?: CaseEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn,
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim(),
      tier = riskRatings?.tier?.value?.level,
      nomsNumber = caseSummary.nomsId,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = OffsetDateTime.now(),
    )
    return caseRepository.saveAndFlush(caseEntity)
  }

  fun getCase(crn: String): CaseDto? = caseRepository.findByCrn(crn)?.let {
    CaseDto(
      crn = it.crn,
      nomsNumber = it.nomsNumber,
      name = it.name,
      tier = it.tier,
      createdAt = it.createdAt,
      lastUpdatedAt = it.lastUpdatedAt,
    )
  }
}
