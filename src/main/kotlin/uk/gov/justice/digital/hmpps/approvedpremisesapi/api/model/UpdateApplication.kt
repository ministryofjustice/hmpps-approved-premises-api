package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Application

/**
 *
 * @param type
 * @param &#x60;data&#x60;
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = UpdateApprovedPremisesApplication::class, name = "CAS1"),
  JsonSubTypes.Type(value = UpdateCas2Application::class, name = "CAS2"),
  JsonSubTypes.Type(value = UpdateCas2v2Application::class, name = "CAS2V2"),
  JsonSubTypes.Type(value = UpdateTemporaryAccommodationApplication::class, name = "CAS3"),
)
interface UpdateApplication {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val type: UpdateApplicationType

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>
}
