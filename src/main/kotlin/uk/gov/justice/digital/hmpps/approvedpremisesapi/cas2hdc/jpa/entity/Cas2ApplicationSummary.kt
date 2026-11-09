package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface Cas2ApplicationSummary {
  val id: UUID
  val crn: String
  val nomsNumber: String?
  val userId: UUID
  val userName: String
  val allocatedPomUserId: UUID?
  val allocatedPomName: String?
  val createdAt: Instant
  val submittedAt: Instant?
  val hdcEligibilityDate: LocalDate?
  val latestStatusUpdateLabel: String?
  val latestStatusUpdateStatusId: UUID?
  val prisonCode: String?
  val currentPrisonCode: String?
  val assignmentDate: Instant?
  val bailHearingDate: LocalDate?
  val applicationOrigin: String?
  val cohort: Cas2Cohort?
}
