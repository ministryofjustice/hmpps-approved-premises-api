package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.UUID

data class Cas3SuitableApplication(
  val id: UUID,
  val applicationStatus: ApplicationStatus,
)
