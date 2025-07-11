package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param staffMember
 */
data class Cas2ApplicationSubmittedEventDetailsSubmittedBy(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("staffMember", required = true) val staffMember: Cas2StaffMember,
)
