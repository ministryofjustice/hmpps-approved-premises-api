package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessment

/**
 *
 * @param service
 * @param id
 * @param schemaVersion
 * @param outdatedSchema
 * @param createdAt
 * @param clarificationNotes
 * @param allocatedAt
 * @param submittedAt
 * @param decision
 * @param rejectionRationale
 * @param &#x60;data&#x60; Any object
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "service", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremisesAssessment::class, name = "CAS1"),
  JsonSubTypes.Type(value = TemporaryAccommodationAssessment::class, name = "CAS3"),
)
interface Assessment {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val service: kotlin.String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val createdAt: java.time.Instant

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val clarificationNotes: kotlin.collections.List<ClarificationNote>

  @get:Schema(example = "null", description = "")
  val allocatedAt: java.time.Instant?

  @get:Schema(example = "null", description = "")
  val submittedAt: java.time.Instant?

  @get:Schema(example = "null", description = "")
  val decision: AssessmentDecision?

  @get:Schema(example = "null", description = "")
  val rejectionRationale: kotlin.String?

  @get:Schema(example = "null", description = "Any object")
  val `data`: kotlin.Any?
}
