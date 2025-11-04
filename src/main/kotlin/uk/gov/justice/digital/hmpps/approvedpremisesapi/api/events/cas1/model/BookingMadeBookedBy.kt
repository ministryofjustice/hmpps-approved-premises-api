package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

/**
 *
 * @param staffMember
 * @param cru
 */
data class BookingMadeBookedBy(

  val staffMember: StaffMember? = null,

  val cru: Cru? = null,
)
