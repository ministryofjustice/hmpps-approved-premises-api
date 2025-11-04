package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param schemaVersion
 * @param outdatedSchema
 * @param timelineEvents
 * @param assessment
 * @param submittedBy
 * @param document Any object
 * @param submittedAt
 * @param telephoneNumber
 * @param applicationOrigin
 * @param bailHearingDate
 */
data class Cas2v2SubmittedApplication(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: List<Cas2TimelineEvent>,

  @get:JsonProperty("assessment", required = true) val assessment: Cas2v2Assessment,

  @get:JsonProperty("submittedBy") val submittedBy: Cas2v2User? = null,

  @get:JsonProperty("document") val document: Any? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)
