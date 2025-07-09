package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: CAS1_IMPORT_SITE_SURVEY_ROOMS,CAS1_IMPORT_SITE_SURVEY_PREMISES
*/
enum class SeedFromExcelFileType(@get:JsonValue val value: kotlin.String) {

    CAS1_IMPORT_SITE_SURVEY_ROOMS("cas1_import_site_survey_rooms"),
    CAS1_IMPORT_SITE_SURVEY_PREMISES("cas1_import_site_survey_premises");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SeedFromExcelFileType {
                return values().first{it -> it.value == value}
        }
    }
}

