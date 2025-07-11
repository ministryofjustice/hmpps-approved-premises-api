package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param service
 * @param id
 * @param name
 * @param deliusUsername
 * @param region
 * @param email
 * @param telephoneNumber
 * @param isActive
 * @param probationDeliveryUnit
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "service", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesUser::class, name = "CAS1"),
  JsonSubTypes.Type(value = TemporaryAccommodationUser::class, name = "CAS3"),
)
interface User {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val service: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val name: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val deliusUsername: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val region: ProbationRegion

  @get:Schema(example = "null", description = "")
  val email: kotlin.String?

  @get:Schema(example = "null", description = "")
  val telephoneNumber: kotlin.String?

  @get:Schema(example = "null", description = "")
  val isActive: kotlin.Boolean?

  @get:Schema(example = "null", description = "")
  val probationDeliveryUnit: ProbationDeliveryUnit?
}
