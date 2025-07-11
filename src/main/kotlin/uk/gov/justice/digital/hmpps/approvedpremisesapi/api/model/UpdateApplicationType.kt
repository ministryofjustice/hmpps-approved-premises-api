package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: CAS1,CAS2,CAS3,CAS2V2
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class UpdateApplicationType(@get:JsonValue val value: kotlin.String) {

  CAS1("CAS1"),
  CAS2("CAS2"),
  CAS3("CAS3"),
  CAS2V2("CAS2V2"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): UpdateApplicationType = entries.first { it.value == value }
  }
}
