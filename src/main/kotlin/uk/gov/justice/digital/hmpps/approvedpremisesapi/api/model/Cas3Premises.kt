package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param reference
 * @param addressLine1
 * @param postcode
 * @param probationRegion
 * @param probationDeliveryUnit
 * @param status
 * @param totalOnlineBedspaces
 * @param totalUpcomingBedspaces
 * @param totalArchivedBedspaces
 * @param addressLine2
 * @param town
 * @param localAuthorityArea
 * @param characteristics
 * @param startDate Start date of the property.
 * @param notes
 * @param turnaroundWorkingDays
 */
data class Cas3Premises(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "Hope House", required = true, description = "")
    @get:JsonProperty("reference", required = true) val reference: String,

    @Schema(example = "one something street", required = true, description = "")
    @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

    @Schema(example = "LS1 3AD", required = true, description = "")
    @get:JsonProperty("postcode", required = true) val postcode: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("probationRegion", required = true) val probationRegion: ProbationRegion,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("probationDeliveryUnit", required = true) val probationDeliveryUnit: ProbationDeliveryUnit,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: Cas3PremisesStatus,

    @Schema(example = "5", required = true, description = "")
    @get:JsonProperty("totalOnlineBedspaces", required = true) val totalOnlineBedspaces: Int,

    @Schema(example = "1", required = true, description = "")
    @get:JsonProperty("totalUpcomingBedspaces", required = true) val totalUpcomingBedspaces: Int,

    @Schema(example = "2", required = true, description = "")
    @get:JsonProperty("totalArchivedBedspaces", required = true) val totalArchivedBedspaces: Int,

    @Schema(example = "Blackmore End", description = "")
    @get:JsonProperty("addressLine2") val addressLine2: String? = null,

    @Schema(example = "Braintree", description = "")
    @get:JsonProperty("town") val town: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("localAuthorityArea") val localAuthorityArea: LocalAuthorityArea? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("characteristics") val characteristics: List<Characteristic>? = null,

    @Schema(example = "Sat Mar 30 00:00:00 GMT 2024", description = "Start date of the property.")
    @get:JsonProperty("startDate") val startDate: LocalDate? = null,

    @Schema(example = "some notes about this property", description = "")
    @get:JsonProperty("notes") val notes: String? = null,

    @Schema(example = "2", description = "")
    @get:JsonProperty("turnaroundWorkingDays") val turnaroundWorkingDays: Int? = null
)

