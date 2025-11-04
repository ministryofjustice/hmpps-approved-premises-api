package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param category
 */
data class ReferralHistorySystemNote(

  val category: ReferralHistorySystemNote.Category,

  override val id: java.util.UUID,

  override val createdAt: java.time.Instant,

  override val createdByUserName: kotlin.String,

  override val type: kotlin.String,

  override val message: kotlin.String? = null,

  override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
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
