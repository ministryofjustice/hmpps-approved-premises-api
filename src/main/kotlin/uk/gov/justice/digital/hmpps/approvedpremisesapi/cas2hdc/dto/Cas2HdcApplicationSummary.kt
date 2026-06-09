package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas2HdcApplicationSummary(

  @get:JsonProperty("type", required = true) val type: String,

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("personName", required = true) val personName: String,

  @get:JsonProperty("crn", required = true) val crn: String,

  @get:JsonProperty("nomsNumber", required = true) val nomsNumber: String,

  @get:JsonProperty("allocatedPomUserId", required = true) val allocatedPomUserId: UUID,

  @get:JsonProperty("allocatedPomName", required = true) val allocatedPomName: String,

  @get:JsonProperty("assignmentDate", required = true) val assignmentDate: LocalDate,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("createdByUserName") val createdByUserName: String? = null,

  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: Cas2HdcLatestStatusUpdate? = null,

  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @get:JsonProperty("currentPrisonName") val currentPrisonName: String? = null,

  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)
