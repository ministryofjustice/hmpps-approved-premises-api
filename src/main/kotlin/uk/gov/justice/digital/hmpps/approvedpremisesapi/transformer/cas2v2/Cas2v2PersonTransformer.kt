package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary

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
  )
}
