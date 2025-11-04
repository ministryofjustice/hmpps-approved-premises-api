package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class ReferralHistoryUserNote(

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("createdByUserName", required = true) override val createdByUserName: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("message") override val message: kotlin.String? = null,

  @get:JsonProperty("messageDetails") override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
) : ReferralHistoryNote
