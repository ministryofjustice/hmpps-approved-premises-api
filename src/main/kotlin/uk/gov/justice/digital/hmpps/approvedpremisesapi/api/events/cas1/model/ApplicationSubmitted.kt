package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

data class ApplicationSubmitted(

  val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: String,

  val personReference: PersonReference,

  @field:Schema(example = "7", description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: String,

  @field:Schema(example = "rotl")
  val releaseType: String,

  @field:Schema(example = "43")
  val age: Int,

  val gender: Gender?,

  @field:Schema(example = "LS2")
  val targetLocation: String,

  @get:JsonProperty("submittedAt", required = true) val submittedAt: java.time.Instant,

  val submittedBy: ApplicationSubmittedSubmittedBy,

  @field:Schema(example = "CAT C3/LEVEL L2")
  val mappa: String? = null,

  @field:Schema(example = "57")
  val sentenceLengthInMonths: Int? = null,

  @field:Schema(example = "AB43782")
  @get:JsonProperty("offenceId") val offenceId: String? = null,
) : Cas1DomainEventPayload {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Gender(@get:JsonValue val value: String) {

    male("Male"),
    female("Female"),
  }
}
