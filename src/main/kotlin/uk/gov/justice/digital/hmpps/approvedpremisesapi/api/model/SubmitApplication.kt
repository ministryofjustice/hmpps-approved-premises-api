package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param translatedDocument Any object that conforms to the current JSON schema for an application
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = SubmitApprovedPremisesApplication::class, name = "CAS1"),
  JsonSubTypes.Type(value = SubmitCas2Application::class, name = "CAS2"),
  JsonSubTypes.Type(value = SubmitTemporaryAccommodationApplication::class, name = "CAS3"),
)
interface SubmitApplication {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: kotlin.String

  @get:Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  val translatedDocument: kotlin.Any?
}
