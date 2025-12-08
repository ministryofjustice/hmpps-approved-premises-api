package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import java.time.LocalDate

@Component
class Cas1PersonTransformer {

  fun transformPersonToCas1PersonDetails(
    personSummaryInfoResult: PersonSummaryInfoResult,
    tier: RiskTier,
  ): Cas1PersonDetails = when (personSummaryInfoResult) {
    is PersonSummaryInfoResult.Success.Full -> transformFullPersonToCas1PersonDetails(personSummaryInfoResult.summary, tier)
    is PersonSummaryInfoResult.Success.Restricted -> transformRestrictedPersonToCas1PersonDetails(personSummaryInfoResult.nomsNumber, tier)
    else -> error("Unexpected PersonSummaryInfoResult type: ${personSummaryInfoResult::class.simpleName}")
  }

  private fun transformFullPersonToCas1PersonDetails(caseSummary: CaseSummary, tier: RiskTier): Cas1PersonDetails = Cas1PersonDetails(
    name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
    dateOfBirth = caseSummary.dateOfBirth,
    nationality = caseSummary.profile?.nationality,
    tier = tier.level,
    nomsId = caseSummary.nomsId,
    pnc = caseSummary.pnc,
    ethnicity = caseSummary.profile?.ethnicity,
    religion = caseSummary.profile?.religion,
    genderIdentity = caseSummary.profile?.genderIdentity,
  )

  private fun transformRestrictedPersonToCas1PersonDetails(nomsNumber: String?, tier: RiskTier): Cas1PersonDetails = Cas1PersonDetails(
    name = "LAO Person",
    dateOfBirth = LocalDate.MIN,
    nationality = "LAO Person",
    tier = tier.level,
    nomsId = nomsNumber,
    pnc = "LAO Person",
    ethnicity = "LAO Person",
    religion = "LAO Person",
    genderIdentity = "LAO Person",
  )
}
