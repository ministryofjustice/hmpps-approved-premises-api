package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param createdAt
 * @param createdByUserName
 * @param type
 * @param message
 * @param messageDetails
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ReferralHistoryDomainEventNote::class, name = "domainEvent"),
  JsonSubTypes.Type(value = ReferralHistorySystemNote::class, name = "system"),
  JsonSubTypes.Type(value = ReferralHistoryUserNote::class, name = "user"),
)
interface ReferralHistoryNote {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val createdAt: java.time.Instant

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val createdByUserName: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: kotlin.String

  @get:Schema(example = "null", description = "")
  val message: kotlin.String?

  @get:Schema(example = "null", description = "")
  val messageDetails: ReferralHistoryNoteMessageDetails?
}
