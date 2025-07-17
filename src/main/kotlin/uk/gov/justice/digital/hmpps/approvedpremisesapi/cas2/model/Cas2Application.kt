package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas2Application(
  val createdBy: NomisUser,
  val status: ApplicationStatus,
  val isTransferredApplication: Boolean,
  val type: String,
  val id: UUID,
  val person: Person,
  val createdAt: Instant,
  val data: Any? = null,
  val document: Any? = null,
  val submittedAt: Instant? = null,
  val telephoneNumber: String? = null,
  val assessment: Cas2Assessment? = null,
  val timelineEvents: List<Cas2TimelineEvent>? = null,
  val allocatedPomName: String? = null,
  val currentPrisonName: String? = null,
  val allocatedPomEmailAddress: String? = null,
  val omuEmailAddress: String? = null,
  val assignmentDate: LocalDate? = null,
  val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,
  val bailHearingDate: LocalDate? = null,
)
