package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param createdByUserId
 * @param crn
 * @param nomsNumber
 * @param personName
 * @param createdAt
 * @param submittedAt
 */
data class Cas2SubmittedApplicationSummary(

  val id: UUID,

  val createdByUserId: UUID,

  val crn: String,

  val nomsNumber: String,

  val personName: String,

  val createdAt: Instant,

  val submittedAt: Instant? = null,
)
