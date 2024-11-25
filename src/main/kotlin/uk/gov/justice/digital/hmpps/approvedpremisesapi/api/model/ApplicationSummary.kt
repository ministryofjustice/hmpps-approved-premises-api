package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param id
 * @param person
 * @param createdAt
 * @param submittedAt
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesApplicationSummary::class, name = "CAS1"),
  JsonSubTypes.Type(value = Cas2ApplicationSummary::class, name = "CAS2"),
  JsonSubTypes.Type(value = TemporaryAccommodationApplicationSummary::class, name = "CAS3"),
  JsonSubTypes.Type(value = OfflineApplicationSummary::class, name = "Offline"),
)
interface ApplicationSummary {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val person: Person

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val createdAt: java.time.Instant

  @get:Schema(example = "null", description = "")
  val submittedAt: java.time.Instant?
}
