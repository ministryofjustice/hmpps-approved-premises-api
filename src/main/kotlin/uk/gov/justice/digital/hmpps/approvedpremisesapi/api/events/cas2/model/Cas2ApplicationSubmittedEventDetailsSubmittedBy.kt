package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas2ApplicationSubmittedEventDetailsSubmittedBy(

  @get:JsonProperty("staffMember", required = true) val staffMember: Cas2StaffMember,
)
