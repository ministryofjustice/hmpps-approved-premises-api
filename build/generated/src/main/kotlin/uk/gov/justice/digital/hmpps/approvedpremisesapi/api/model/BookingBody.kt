package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param person 
 * @param arrivalDate 
 * @param originalArrivalDate 
 * @param departureDate 
 * @param originalDepartureDate 
 * @param createdAt 
 * @param serviceName 
 * @param keyWorker KeyWorker is a legacy field only used by CAS1. It is not longer being captured or populated
 * @param bed 
 */
data class BookingBody(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceName", required = true) val serviceName: ServiceName,

    @Schema(example = "null", description = "KeyWorker is a legacy field only used by CAS1. It is not longer being captured or populated")
    @Deprecated(message = "")
    @get:JsonProperty("keyWorker") val keyWorker: StaffMember? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bed") val bed: Bed? = null
) {

}

