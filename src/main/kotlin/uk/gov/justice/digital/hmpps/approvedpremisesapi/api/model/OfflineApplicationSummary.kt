package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class OfflineApplicationSummary(

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary
