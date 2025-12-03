package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import java.time.LocalDate

@Component
class Cas1PersonTransformer {

  fun transformFullPersonToCas1PersonDetails(caseSummary: CaseSummary, tierLevel: String?): Cas1PersonDetails = Cas1PersonDetails(
    name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
    alias = "Dummy Value",
    dateOfBirth = caseSummary.dateOfBirth,
    nationality = caseSummary.profile?.nationality,
    immigrationStatus = "Dummy Value",
    languages = "Dummy Value",
    relationshipStatus = "Dummy Value",
    dependants = "Dummy Value",
    disabilities = "Dummy Value",
    tier = tierLevel,
    nomsId = caseSummary.nomsId,
    pnc = caseSummary.pnc,
    ethnicity = caseSummary.profile?.ethnicity,
    religion = caseSummary.profile?.religion,
    sex = "Dummy Value",
    genderIdentity = caseSummary.profile?.genderIdentity,
    sexualOrientation = "Dummy Value",
  )

  fun transformRestrictedPersonToCas1PersonDetails(nomsNumber: String?, tierLevel: String?): Cas1PersonDetails = Cas1PersonDetails(
    name = "LAO Person",
    alias = "LAO Person",
    dateOfBirth = LocalDate.MIN,
    nationality = "LAO Person",
    immigrationStatus = "LAO Person",
    languages = "LAO Person",
    relationshipStatus = "LAO Person",
    dependants = "LAO Person",
    disabilities = "LAO Person",
    tier = tierLevel,
    nomsId = nomsNumber,
    pnc = "LAO Person",
    ethnicity = "LAO Person",
    religion = "LAO Person",
    sex = "LAO Person",
    genderIdentity = "LAO Person",
    sexualOrientation = "Dummy Value",
    )
}
