package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationPremises

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "service", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(value = ApprovedPremises::class, name = "CAS1"),
  JsonSubTypes.Type(value = TemporaryAccommodationPremises::class, name = "CAS3"),
)
interface Premises {
  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val service: String

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val id: java.util.UUID

  @get:Schema(example = "Hope House", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val name: String

  @get:Schema(example = "one something street", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val addressLine1: String

  @get:Schema(example = "LS1 3AD", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val postcode: String

  @get:Schema(example = "22", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val bedCount: Int

  @get:Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val availableBedsForToday: Int

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val probationRegion: ProbationRegion

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val apArea: ApArea

  @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
  val status: PropertyStatus

  @get:Schema(example = "Blackmore End", description = "")
  val addressLine2: String?

  @get:Schema(example = "Braintree", description = "")
  val town: String?

  @get:Schema(example = "some notes about this property", description = "")
  val notes: String?

  @get:Schema(example = "null", description = "")
  val localAuthorityArea: LocalAuthorityArea?

  @get:Schema(example = "null", description = "")
  val characteristics: List<Characteristic>?
}
