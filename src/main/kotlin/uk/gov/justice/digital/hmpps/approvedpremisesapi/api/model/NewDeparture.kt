package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewDeparture(

  @get:JsonProperty("dateTime", required = true) val dateTime: java.time.Instant,

  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @get:JsonProperty("moveOnCategoryId", required = true) val moveOnCategoryId: java.util.UUID,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("destinationProviderId") val destinationProviderId: java.util.UUID? = null,
)
