package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param category
 */
data class ReferralHistorySystemNote(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("category", required = true) val category: ReferralHistorySystemNote.Category,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserName", required = true) override val createdByUserName: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("message") override val message: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("messageDetails") override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
) : ReferralHistoryNote {

  /**
   *
   * Values: submitted,unallocated,inReview,readyToPlace,rejected,completed
   */
  enum class Category(val value: kotlin.String) {

    @JsonProperty("submitted")
    submitted("submitted"),

    @JsonProperty("unallocated")
    unallocated("unallocated"),

    @JsonProperty("in_review")
    inReview("in_review"),

    @JsonProperty("ready_to_place")
    readyToPlace("ready_to_place"),

    @JsonProperty("rejected")
    rejected("rejected"),

    @JsonProperty("completed")
    completed("completed"),
  }
}
