package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas2v2ApplicationSummary(

  val type: String,

  val id: UUID,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val status: ApplicationStatus,

  val personName: String,

  val crn: String,

  val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  val submittedAt: Instant? = null,

  val createdByUserName: String? = null,

  val latestStatusUpdate: LatestCas2v2StatusUpdate? = null,

  val risks: PersonRisks? = null,

  val hdcEligibilityDate: LocalDate? = null,

  val nomsNumber: String? = null,

  val bailHearingDate: LocalDate? = null,

  val prisonCode: String? = null,
)
