package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus

/**
 *
 * @param pdu
 * @param probationDeliveryUnit
 * @param turnaroundWorkingDayCount
 */
data class TemporaryAccommodationPremises(

  @get:JsonProperty("pdu", required = true) val pdu: kotlin.String,

  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @field:Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) override val name: kotlin.String,

  @field:Schema(example = "one something street", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) override val addressLine1: kotlin.String,

  @field:Schema(example = "LS1 3AD", required = true, description = "")
  @get:JsonProperty("postcode", required = true) override val postcode: kotlin.String,

  @field:Schema(example = "22", required = true, description = "")
  @get:JsonProperty("bedCount", required = true) override val bedCount: kotlin.Int,

  @field:Schema(example = "20", required = true, description = "")
  @get:JsonProperty("availableBedsForToday", required = true) override val availableBedsForToday: kotlin.Int,

  @get:JsonProperty("probationRegion", required = true) override val probationRegion: ProbationRegion,

  @get:JsonProperty("apArea", required = true) override val apArea: ApArea,

  @get:JsonProperty("status", required = true) override val status: PropertyStatus,

  @get:JsonProperty("probationDeliveryUnit") val probationDeliveryUnit: ProbationDeliveryUnit? = null,

  @field:Schema(example = "2", description = "")
  @get:JsonProperty("turnaroundWorkingDayCount") val turnaroundWorkingDayCount: kotlin.Int? = null,

  @field:Schema(example = "Blackmore End", description = "")
  @get:JsonProperty("addressLine2") override val addressLine2: kotlin.String? = null,

  @field:Schema(example = "Braintree", description = "")
  @get:JsonProperty("town") override val town: kotlin.String? = null,

  @field:Schema(example = "some notes about this property", description = "")
  @get:JsonProperty("notes") override val notes: kotlin.String? = null,

  @get:JsonProperty("localAuthorityArea") override val localAuthorityArea: LocalAuthorityArea? = null,

  @get:JsonProperty("characteristics") override val characteristics: kotlin.collections.List<Characteristic>? = null,
) : Premises
