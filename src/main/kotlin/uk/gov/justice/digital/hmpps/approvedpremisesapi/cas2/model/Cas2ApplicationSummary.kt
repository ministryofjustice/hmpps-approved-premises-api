package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
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

  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: LatestCas2StatusUpdate? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @get:JsonProperty("currentPrisonName") val currentPrisonName: String? = null,

  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)
