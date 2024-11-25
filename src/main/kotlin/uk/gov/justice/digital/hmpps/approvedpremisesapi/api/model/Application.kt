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
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesApplication::class, name = "CAS1"),
  JsonSubTypes.Type(value = Cas2Application::class, name = "CAS2"),
  JsonSubTypes.Type(value = TemporaryAccommodationApplication::class, name = "CAS3"),
  JsonSubTypes.Type(value = OfflineApplication::class, name = "Offline"),
)
interface Application {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val person: Person

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val createdAt: java.time.Instant
}
