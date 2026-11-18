package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface Cas2ApplicationSummary {
  val id: UUID
  val crn: String
  val nomsNumber: String?
  val userId: String
  val userName: String
  val allocatedPomUserId: UUID?
  val allocatedPomName: String?
  val createdAt: OffsetDateTime
  val submittedAt: OffsetDateTime?
  val abandonedAt: OffsetDateTime?
  val hdcEligibilityDate: LocalDate?
  val latestStatusUpdateLabel: String?
  val latestStatusUpdateStatusId: String?
  val prisonCode: String?
  val currentPrisonCode: String?
  val assignmentDate: OffsetDateTime?
  val bailHearingDate: LocalDate?
  val applicationOrigin: String?
  val serviceOrigin: String?
  val cohort: Cas2Cohort?
}
