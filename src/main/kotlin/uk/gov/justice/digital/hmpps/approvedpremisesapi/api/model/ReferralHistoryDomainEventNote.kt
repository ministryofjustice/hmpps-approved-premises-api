package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class ReferralHistoryDomainEventNote(

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("createdByUserName", required = true) override val createdByUserName: String,

  @get:JsonProperty("type", required = true) override val type: String,

  @get:JsonProperty("message") override val message: String? = null,

  @get:JsonProperty("messageDetails") override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
) : ReferralHistoryNote
