package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param service
 * @param id
 * @param name
 * @param addressLine1
 * @param postcode
 * @param bedCount
 * @param status
 * @param addressLine2
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "service", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesSummary::class, name = "CAS1"),
  JsonSubTypes.Type(value = TemporaryAccommodationPremisesSummary::class, name = "CAS3"),
)
interface PremisesSummary {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val service: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "Hope House", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val name: kotlin.String

  @get:Schema(example = "one something street", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val addressLine1: kotlin.String

  @get:Schema(example = "LS1 3AD", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val postcode: kotlin.String

  @get:Schema(example = "22", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val bedCount: kotlin.Int

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val status: PropertyStatus

  @get:Schema(example = "Blackmore End", description = "")
  val addressLine2: kotlin.String?
}
