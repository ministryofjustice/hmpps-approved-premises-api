package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * 
 * @param arrivalDate 
 */
data class NewCas2Arrival(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedDepartureDate", required = true) override val expectedDepartureDate: LocalDate,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") override val notes: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("keyWorkerStaffCode") override val keyWorkerStaffCode: String? = null
    ) : NewArrival{

}

