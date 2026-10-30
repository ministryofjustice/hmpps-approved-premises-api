package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name

@Component
class Cas2PersonTransformer {

  fun transformCaseSummaryToFullPerson(caseSummary: CaseSummary, tier: TierDto?): FullPerson = FullPerson(
    name = transformCaseSummaryNameToFullName(caseSummary.name),
    dateOfBirth = caseSummary.dateOfBirth,
    sex = caseSummary.gender ?: "Unknown",
    status = PersonStatus.unknown,
    crn = caseSummary.crn,
    type = PersonType.fullPerson,
    nomsNumber = caseSummary.nomsId,
    pncNumber = caseSummary.pnc,
    nationality = caseSummary.profile?.nationality ?: "Unknown",
    tier = tier,
  )

  fun transformCaseSummaryNameToFullName(name: Name): String = "${name.forename} ${name.surname}"
}
