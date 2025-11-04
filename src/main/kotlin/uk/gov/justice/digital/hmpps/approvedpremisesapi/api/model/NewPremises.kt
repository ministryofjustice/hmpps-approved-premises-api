package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 * @param addressLine1
 * @param postcode
 * @param probationRegionId
 * @param characteristicIds
 * @param status
 * @param addressLine2
 * @param town
 * @param notes
 * @param localAuthorityAreaId
 * @param pdu
 * @param probationDeliveryUnitId
 * @param turnaroundWorkingDayCount
 */
data class NewPremises(

  val name: kotlin.String,

  val addressLine1: kotlin.String,

  val postcode: kotlin.String,

  val probationRegionId: java.util.UUID,

  val characteristicIds: kotlin.collections.List<java.util.UUID>,

  val status: PropertyStatus,

  val addressLine2: kotlin.String? = null,

  val town: kotlin.String? = null,

  @Schema(example = "some notes about this property", description = "")
  val notes: kotlin.String? = null,

  val localAuthorityAreaId: java.util.UUID? = null,

  val pdu: kotlin.String? = null,

  val probationDeliveryUnitId: java.util.UUID? = null,

  val turnaroundWorkingDayCount: kotlin.Int? = null,
)
