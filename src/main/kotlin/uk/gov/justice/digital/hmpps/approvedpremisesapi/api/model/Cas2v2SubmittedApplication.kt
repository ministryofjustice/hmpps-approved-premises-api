package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas2v2SubmittedApplication(

  val id: UUID,

  val person: Person,

  val createdAt: Instant,

  val timelineEvents: List<Cas2TimelineEvent>,

  val assessment: Cas2v2Assessment,

  val submittedBy: Cas2v2User? = null,

  val document: Any? = null,

  val submittedAt: Instant? = null,

  val telephoneNumber: String? = null,

  val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  val bailHearingDate: LocalDate? = null,
)
