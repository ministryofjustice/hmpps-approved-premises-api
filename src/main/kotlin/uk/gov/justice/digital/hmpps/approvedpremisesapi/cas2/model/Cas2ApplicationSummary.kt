package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param type
 * @param id
 * @param createdAt
 * @param createdByUserId
 * @param status
 * @param personName
 * @param crn
 * @param nomsNumber
 * @param allocatedPomUserId
 * @param allocatedPomName
 * @param assignmentDate
 * @param submittedAt
 * @param createdByUserName
 * @param latestStatusUpdate
 * @param risks
 * @param hdcEligibilityDate
 * @param currentPrisonName
 * @param applicationOrigin
 * @param bailHearingDate
 */
data class Cas2ApplicationSummary(

  val type: String,

  val id: UUID,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val status: ApplicationStatus,

  val personName: String,

  val crn: String,

  val nomsNumber: String,

  val allocatedPomUserId: UUID,

  val allocatedPomName: String,

  val assignmentDate: LocalDate,

  val submittedAt: Instant? = null,

  val createdByUserName: String? = null,

  val latestStatusUpdate: LatestCas2StatusUpdate? = null,

  val risks: PersonRisks? = null,

  val hdcEligibilityDate: LocalDate? = null,

  val currentPrisonName: String? = null,

  val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  val bailHearingDate: LocalDate? = null,
)
