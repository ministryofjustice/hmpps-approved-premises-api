package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProfileFactory

fun OffenderDetailSummary.asCaseDetail() = CaseDetailFactory().withCase(
  CaseSummaryFactory()
    .withCrn(this.otherIds.crn)
    .withNomsId(this.otherIds.nomsNumber)
    .withGender(this.gender)
    .withPnc(this.otherIds.pncNumber)
    .withName(
      NameFactory()
        .withForename(this.firstName)
        .withSurname(this.surname)
        .withMiddleNames(
          this.middleNames?.let {
            this.middleNames
          } ?: emptyList(),
        )
        .produce(),
    )
    .withDateOfBirth(this.dateOfBirth)
    .withProfile(
      ProfileFactory()
        .withReligion(this.offenderProfile.religion)
        .withEthnicity(this.offenderProfile.ethnicity)
        .withNationality(this.offenderProfile.nationality)
        .withGenderIdentity(
          when (this.offenderProfile.genderIdentity) {
            "Prefer to self-describe" -> this.offenderProfile.selfDescribedGender
            else -> this.offenderProfile.genderIdentity
          },
        )
        .produce(),
    )
    .withCurrentExclusion(this.currentExclusion)
    .withCurrentRestriction(this.currentRestriction)
    .produce(),
).produce()
