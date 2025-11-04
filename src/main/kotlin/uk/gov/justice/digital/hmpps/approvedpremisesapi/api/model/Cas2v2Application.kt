package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param createdBy
 * @param schemaVersion
 * @param outdatedSchema
 * @param status
 * @param applicationOrigin
 * @param &#x60;data&#x60; Any object
 * @param document Any object
 * @param submittedAt
 * @param telephoneNumber
 * @param assessment
 * @param timelineEvents
 * @param bailHearingDate
 */
data class Cas2v2Application(

  @get:JsonProperty("createdBy", required = true) val createdBy: Cas2v2User,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("applicationOrigin", required = true) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("type", required = true) override val type: String,

  @get:JsonProperty("id", required = true) override val id: UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

  @get:JsonProperty("data") val `data`: Any? = null,

  @get:JsonProperty("document") val document: Any? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @get:JsonProperty("assessment") val assessment: Cas2v2Assessment? = null,

  @get:JsonProperty("timelineEvents") val timelineEvents: List<Cas2TimelineEvent>? = null,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
) : Application
