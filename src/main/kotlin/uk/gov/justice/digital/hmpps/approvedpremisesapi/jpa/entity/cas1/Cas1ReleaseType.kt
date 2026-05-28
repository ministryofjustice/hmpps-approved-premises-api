package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption

// This enum has been created to replace usage of [String] on the entity model
// as these values were previously populated by the API Enumeration [ReleaseTypeOption]
// we have maintained the (incorrect) naming style
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ReleaseType(
  val apiType: ReleaseTypeOption,
) {
  licence(ReleaseTypeOption.licence),
  rotl(ReleaseTypeOption.rotl),
  hdc(ReleaseTypeOption.hdc),
  pss(ReleaseTypeOption.pss),
  inCommunity(ReleaseTypeOption.inCommunity),
  notApplicable(ReleaseTypeOption.notApplicable),
  extendedDeterminateLicence(ReleaseTypeOption.extendedDeterminateLicence),
  paroleDirectedLicence(ReleaseTypeOption.paroleDirectedLicence),
  reReleasedPostRecall(ReleaseTypeOption.reReleasedPostRecall),
  reReleasedFollowingFixedTermRecall(ReleaseTypeOption.reReleasedFollowingFixedTermRecall),
  ;

  companion object {
    fun fromApiType(apiType: ReleaseTypeOption) = Cas1ReleaseType.entries.firstOrNull { it.apiType == apiType } ?: error("Can't find entry for API Type $apiType")
  }
}
