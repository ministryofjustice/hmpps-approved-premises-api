package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewOutOfServiceBedCancellation(

  @get:JsonProperty("notes") val notes: String? = null,
)
