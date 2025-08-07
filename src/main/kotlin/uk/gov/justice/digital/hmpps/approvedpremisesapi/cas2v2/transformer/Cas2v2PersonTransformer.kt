package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary

@Component
class Cas2v2PersonTransformer {

  fun transformCaseSummaryToFullPerson(caseSummary: CaseSummary): FullPerson = FullPerson(
    name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
    dateOfBirth = caseSummary.dateOfBirth,
    sex = caseSummary.gender ?: "Unknown",
    status = PersonStatus.unknown,
    crn = caseSummary.crn,
    type = PersonType.fullPerson,
    nomsNumber = caseSummary.nomsId,
    pncNumber = caseSummary.pnc,
    nationality = caseSummary.profile?.nationality ?: "Unknown",
  )
}
