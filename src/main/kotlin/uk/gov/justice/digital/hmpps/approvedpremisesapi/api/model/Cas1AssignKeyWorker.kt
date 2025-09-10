package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

/**
 * Whilst transitioning to using users to represent key workers,
 * either the staff code or user id can be provided
 */
data class Cas1AssignKeyWorker(
  val userId: UUID?,
)
