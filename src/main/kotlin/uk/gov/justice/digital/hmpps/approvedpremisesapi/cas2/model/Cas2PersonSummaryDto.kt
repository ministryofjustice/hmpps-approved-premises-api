package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "personType", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = Cas2PersonSummaryDto.Found::class, name = "Found"),
  JsonSubTypes.Type(value = Cas2PersonSummaryDto.NotFound::class, name = "NotFound"),
  JsonSubTypes.Type(value = Cas2PersonSummaryDto.LimitedAccess::class, name = "LimitedAccess"),
)
sealed interface Cas2PersonSummaryDto {
  @get:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  val crn: String

  @get:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  val personType: Cas2PersonSummaryDtoType

  data class Found(override val crn: String, val name: String) : Cas2PersonSummaryDto {
    override val personType = Cas2PersonSummaryDtoType.found
  }

  data class NotFound(override val crn: String) : Cas2PersonSummaryDto {
    override val personType = Cas2PersonSummaryDtoType.notFound
  }

  data class LimitedAccess(override val crn: String) : Cas2PersonSummaryDto {
    override val personType = Cas2PersonSummaryDtoType.limitedAccess
  }
}

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas2PersonSummaryDtoType(@get:JsonValue val value: String) {
  found("Found"),
  notFound("NotFound"),
  limitedAccess("LimitedAccess"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas2PersonSummaryDtoType = entries.first { it.value == value }
  }
}
