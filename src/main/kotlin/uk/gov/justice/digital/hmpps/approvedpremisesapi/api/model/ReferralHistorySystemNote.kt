package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNoteMessageDetails

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
