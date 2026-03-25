package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Models representing the structure expected by template_hmpps-approved-premises-api.mustache
 * used for Subject Access Request (SAR) generation.
 */

data class SarContent(
  @JsonProperty("ApprovedPremises") val approvedPremises: List<Cas1SarData>? = null,
  @JsonProperty("TemporaryAccommodation") val temporaryAccommodation: List<Cas3SarData>? = null,
  @JsonProperty("ShortTermAccommodation") val shortTermAccommodation: List<Cas2SarData>? = null,
  @JsonProperty("BailAccommodation") val bailAccommodation: List<Cas2v2SarData>? = null,
)
