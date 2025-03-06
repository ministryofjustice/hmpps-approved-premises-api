package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1OffenderService(
  private val cas1OffenderRepository: Cas1OffenderRepository,
) {

  fun getOrCreateOffender(caseSummary: CaseSummary, riskRatings: PersonRisks?): Cas1OffenderEntity {
    val offender = cas1OffenderRepository.findByCrn(caseSummary.crn)?.apply {
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}"
      tier = riskRatings?.tier?.value?.level
      nomsNumber = caseSummary.nomsId
    } ?: Cas1OffenderEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn,
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}",
      tier = riskRatings?.tier?.value?.level,
      nomsNumber = caseSummary.nomsId,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = OffsetDateTime.now(),
    )
    return cas1OffenderRepository.saveAndFlush(offender)
  }
}
