package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param staffMember
 * @param cru
 */
data class BookingMadeBookedBy(

  @get:JsonProperty("staffMember") val staffMember: StaffMember? = null,

  @get:JsonProperty("cru") val cru: Cru? = null,
)
