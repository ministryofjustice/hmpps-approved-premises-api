package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param reference
 * @param addressLine1
 * @param postcode
 * @param probationRegionId
 * @param probationDeliveryUnitId
 * @param characteristicIds
 * @param addressLine2
 * @param town
 * @param localAuthorityAreaId
 * @param notes
 * @param turnaroundWorkingDays
 */
data class Cas3NewPremises(

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reference", required = true) val reference: String,

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("postcode", required = true) val postcode: String,

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationRegionId", required = true) val probationRegionId: UUID,

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationDeliveryUnitId", required = true) val probationDeliveryUnitId: UUID,

    @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristicIds", required = true) val characteristicIds: List<UUID>,

    @Schema(example = "null", description = "")
  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

    @Schema(example = "null", description = "")
  @get:JsonProperty("town") val town: String? = null,

    @Schema(example = "null", description = "")
  @get:JsonProperty("localAuthorityAreaId") val localAuthorityAreaId: UUID? = null,

    @Schema(example = "some notes about this property", description = "")
  @get:JsonProperty("notes") val notes: String? = null,

    @Schema(example = "null", description = "")
  @get:JsonProperty("turnaroundWorkingDays") val turnaroundWorkingDays: Int? = null,
)