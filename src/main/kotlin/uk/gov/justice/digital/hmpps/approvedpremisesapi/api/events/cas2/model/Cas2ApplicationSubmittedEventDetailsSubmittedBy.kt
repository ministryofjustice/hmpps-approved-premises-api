package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param staffMember
 */
data class Cas2ApplicationSubmittedEventDetailsSubmittedBy(

  val staffMember: Cas2StaffMember,
)
