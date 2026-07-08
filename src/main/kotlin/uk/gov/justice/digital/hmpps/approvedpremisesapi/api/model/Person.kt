package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = FullPerson::class, name = "FullPerson"),
  JsonSubTypes.Type(value = RestrictedPerson::class, name = "RestrictedPerson"),
  JsonSubTypes.Type(value = UnknownPerson::class, name = "UnknownPerson"),
)
interface Person {
  @get:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  val crn: String

  @get:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  val type: PersonType
}
