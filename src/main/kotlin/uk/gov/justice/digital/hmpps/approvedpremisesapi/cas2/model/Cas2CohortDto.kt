package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas2CohortDto(@get:JsonValue val value: String) {
  HOME_DETENTION_CURFEW("hdc"),
  PRISON_BAIL("prisonBail"),
  COURT_BAIL("courtBail"),
  ALTERNATIVE_TO_CUSTODIAL_RECALL("atcr"),
  HOMELESS_AT_CONDITIONAL_RELEASE_DATE("hcrd"),
  HOMELESS_AT_END_OF_FIXED_TERM_RECALL("hefr"),
  INTENSIVE_SUPERVISION_COURTS("isc"),
  RISK_ASSESSED_RECALL_REVIEW("rarr"),
  REFERRAL_FROM_APPROVED_PREMISES("from_ap"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String) = values().first { it.value == value }
  }
}
