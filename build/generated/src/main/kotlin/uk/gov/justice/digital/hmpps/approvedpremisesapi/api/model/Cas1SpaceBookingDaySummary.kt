package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param person 
 * @param canonicalArrivalDate actual arrival date or, if not known, the expected arrival date
 * @param canonicalDepartureDate actual departure date or, if not known, the expected departure date
 * @param tier Risk rating tier level of corresponding application
 * @param releaseType 
 * @param essentialCharacteristics 
 */
data class Cas1SpaceBookingDaySummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: PersonSummary,

    @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
    @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
    @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "Risk rating tier level of corresponding application")
    @get:JsonProperty("tier", required = true) val tier: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("releaseType", required = true) val releaseType: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("essentialCharacteristics", required = true) val essentialCharacteristics: kotlin.collections.List<Cas1SpaceBookingCharacteristic>
) {

}

