package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param personReference
 * @param deliusEventNumber
 * @param bookingId
 * @param bookingUrl
 * @param premises
 * @param arrivedAt
 * @param expectedDepartureOn
 * @param notes
 * @param applicationId
 * @param applicationUrl
 * @param recordedBy
 */
data class CAS3PersonArrivedEventDetails(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: Premises,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivedAt", required = true) val arrivedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedDepartureOn", required = true) val expectedDepartureOn: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("notes", required = true) val notes: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("recordedBy") val recordedBy: StaffMember? = null,
)
