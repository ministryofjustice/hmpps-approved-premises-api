package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class ReleaseTypeOption(@get:JsonValue val value: kotlin.String) {

  licence("licence"),
  rotl("rotl"),
  hdc("hdc"),
  pss("pss"),
  inCommunity("in_community"),
  notApplicable("not_applicable"),
  extendedDeterminateLicence("extendedDeterminateLicence"),
  paroleDirectedLicence("paroleDirectedLicence"),
  reReleasedPostRecall("reReleasedPostRecall"),
  reReleasedFollowingFixedTermRecall("reReleasedFollowingFixedTermRecall"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): ReleaseTypeOption = values().first { it -> it.value == value }
  }
}
