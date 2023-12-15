package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult

fun getNameFromOffenderDetailSummaryResult(result: AuthorisableActionResult<OffenderDetailSummary>) = when (result) {
  is AuthorisableActionResult.Success -> {
    "${result.entity.firstName} ${result.entity.surname}"
  }
  is AuthorisableActionResult.NotFound -> "Unknown"
  is AuthorisableActionResult.Unauthorised -> "LAO Offender"
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
