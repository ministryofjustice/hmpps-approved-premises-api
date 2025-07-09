package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param seedType 
 * @param directoryName Directory within the pre-configured seed directory
 */
data class SeedFromExcelDirectoryRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("seedType", required = true) val seedType: SeedFromExcelFileType,

    @Schema(example = "null", required = true, description = "Directory within the pre-configured seed directory")
    @get:JsonProperty("directoryName", required = true) val directoryName: kotlin.String
    ) {

}

