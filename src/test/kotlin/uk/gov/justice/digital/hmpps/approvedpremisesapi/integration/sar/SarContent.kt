package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar

/**
 * Models representing the structure expected by hmpps-approved-premises-api.mustache
 * used for Subject Access Request (SAR) generation.
 */

data class SarContent(
  val content: List<Map<String, Any>>? = null,
)
