package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class ReferralHistorySystemNote(

  @get:JsonProperty("category", required = true) val category: ReferralHistorySystemNote.Category,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("createdByUserName", required = true) override val createdByUserName: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("message") override val message: kotlin.String? = null,

  @get:JsonProperty("messageDetails") override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
) : ReferralHistoryNote {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Category(@get:JsonValue val value: kotlin.String) {

    submitted("submitted"),
    unallocated("unallocated"),
    inReview("in_review"),
    readyToPlace("ready_to_place"),
    rejected("rejected"),
    completed("completed"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: kotlin.String): Category = values().first { it -> it.value == value }
    }
  }
}
