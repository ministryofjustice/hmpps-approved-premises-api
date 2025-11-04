package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class ApprovedPremises(

  @Schema(example = "NEHOPE1", required = true, description = "")
  val apCode: kotlin.String,

  override val service: kotlin.String,

  override val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  override val name: kotlin.String,

  @Schema(example = "one something street", required = true, description = "")
  override val addressLine1: kotlin.String,

  @Schema(example = "LS1 3AD", required = true, description = "")
  override val postcode: kotlin.String,

  @Schema(example = "22", required = true, description = "")
  override val bedCount: kotlin.Int,

  @Schema(example = "20", required = true, description = "")
  override val availableBedsForToday: kotlin.Int,

  override val probationRegion: ProbationRegion,

  override val apArea: ApArea,

  override val localAuthorityArea: LocalAuthorityArea,

  override val status: PropertyStatus,

  @Schema(example = "Blackmore End", description = "")
  override val addressLine2: kotlin.String? = null,

  @Schema(example = "Braintree", description = "")
  override val town: kotlin.String? = null,

  @Schema(example = "some notes about this property", description = "")
  override val notes: kotlin.String? = null,

  override val characteristics: kotlin.collections.List<Characteristic>? = null,
) : Premises
