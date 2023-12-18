package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team

fun getNameFromPersonSummaryInfoResult(result: PersonSummaryInfoResult): String = when (result) {
  is PersonSummaryInfoResult.Success.Full -> {
    listOf(
      listOf(result.summary.name.forename),
      result.summary.name.middleNames,
      listOf(result.summary.name.surname),
    ).flatten().joinToString(" ")
  }
  is PersonSummaryInfoResult.Success.Restricted -> {
    "Limited Access Offender"
  }
  is PersonSummaryInfoResult.NotFound -> {
    "Unknown"
  }
  is PersonSummaryInfoResult.Unknown -> {
    "Unknown"
  }
}

fun OffenderDetailSummary.asCaseSummary() = CaseSummary(
  crn = this.otherIds.crn,
  nomsId = this.otherIds.nomsNumber,
  pnc = this.otherIds.pncNumber,
  name = Name(
    forename = this.firstName,
    surname = this.surname,
    middleNames = this.middleNames ?: emptyList(),
  ),
  dateOfBirth = this.dateOfBirth,
  gender = this.gender,
  profile = Profile(
    ethnicity = this.offenderProfile.ethnicity,
    genderIdentity = this.offenderProfile.genderIdentity,
    nationality = this.offenderProfile.nationality,
    religion = this.offenderProfile.religion,
  ),
  manager = Manager(
    team = Team(
      code = "",
      name = "",
      ldu = Ldu(
        code = "",
        name = "",
      ),
    ),
  ),
  currentExclusion = this.currentExclusion,
  currentRestriction = this.currentRestriction,
)
