package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param personReference 
 * @param deliusEventNumber 
 * @param bookingId 
 * @param bookingUrl 
 * @param premises 
 * @param departedAt 
 * @param reason 
 * @param notes 
 * @param applicationId 
 * @param applicationUrl 
 * @param reasonDetail 
 * @param recordedBy 
 */
data class CAS3PersonDepartedEventDetails(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) val premises: Premises,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("departedAt", required = true) val departedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notes", required = true) val notes: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("reasonDetail") val reasonDetail: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("recordedBy") val recordedBy: StaffMember? = null
) {

}

