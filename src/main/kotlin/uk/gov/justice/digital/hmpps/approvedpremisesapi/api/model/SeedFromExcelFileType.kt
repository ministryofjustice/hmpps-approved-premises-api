package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: CAS1_IMPORT_SITE_SURVEY_ROOMS,CAS1_IMPORT_SITE_SURVEY_PREMISES
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SeedFromExcelFileType(@get:JsonValue val value: kotlin.String) {

  CAS1_IMPORT_SITE_SURVEY_ROOMS("cas1_import_site_survey_rooms"),
  CAS1_IMPORT_SITE_SURVEY_PREMISES("cas1_import_site_survey_premises"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): SeedFromExcelFileType = values().first { it -> it.value == value }
  }
}
