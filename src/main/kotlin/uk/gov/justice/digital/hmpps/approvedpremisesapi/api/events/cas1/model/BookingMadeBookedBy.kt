package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember

/**
 *
 * @param staffMember
 * @param cru
 */
data class BookingMadeBookedBy(

  @Schema(example = "null", description = "")
  @get:JsonProperty("staffMember") val staffMember: StaffMember? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cru") val cru: Cru? = null,
)
