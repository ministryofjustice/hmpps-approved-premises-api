package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

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

  val createdBy: Cas2v2User,

  val status: ApplicationStatus,

  val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  override val type: String,

  override val id: UUID,

  override val person: Person,

  override val createdAt: Instant,

  val `data`: Any? = null,

  val document: Any? = null,

  val submittedAt: Instant? = null,

  val telephoneNumber: String? = null,

  val assessment: Cas2v2Assessment? = null,

  val timelineEvents: List<Cas2TimelineEvent>? = null,

  val bailHearingDate: LocalDate? = null,
) : Application
