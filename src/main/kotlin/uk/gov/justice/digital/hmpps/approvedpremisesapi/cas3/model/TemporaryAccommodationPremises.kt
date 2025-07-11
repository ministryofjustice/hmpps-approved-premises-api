package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 
 * @param pdu 
 * @param probationDeliveryUnit 
 * @param turnaroundWorkingDayCount 
 */
data class TemporaryAccommodationPremises(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("pdu", required = true) val pdu: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("service", required = true) override val service: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "Hope House", required = true, description = "")
    @get:JsonProperty("name", required = true) override val name: String,

    @Schema(example = "one something street", required = true, description = "")
    @get:JsonProperty("addressLine1", required = true) override val addressLine1: String,

    @Schema(example = "LS1 3AD", required = true, description = "")
    @get:JsonProperty("postcode", required = true) override val postcode: String,

    @Schema(example = "22", required = true, description = "")
    @get:JsonProperty("bedCount", required = true) override val bedCount: Int,

    @Schema(example = "20", required = true, description = "")
    @get:JsonProperty("availableBedsForToday", required = true) override val availableBedsForToday: Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("probationRegion", required = true) override val probationRegion: ProbationRegion,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("apArea", required = true) override val apArea: ApArea,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) override val status: PropertyStatus,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnit") val probationDeliveryUnit: ProbationDeliveryUnit? = null,

    @Schema(example = "2", description = "")
    @get:JsonProperty("turnaroundWorkingDayCount") val turnaroundWorkingDayCount: Int? = null,

    @Schema(example = "Blackmore End", description = "")
    @get:JsonProperty("addressLine2") override val addressLine2: String? = null,

    @Schema(example = "Braintree", description = "")
    @get:JsonProperty("town") override val town: String? = null,

    @Schema(example = "some notes about this property", description = "")
    @get:JsonProperty("notes") override val notes: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("localAuthorityArea") override val localAuthorityArea: LocalAuthorityArea? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("characteristics") override val characteristics: List<Characteristic>? = null
    ) : Premises{

}

