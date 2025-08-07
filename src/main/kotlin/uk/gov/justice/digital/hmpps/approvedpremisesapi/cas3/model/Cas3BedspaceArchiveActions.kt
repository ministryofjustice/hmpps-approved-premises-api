package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.util.UUID

class Cas3BedspaceArchiveActions(
  val bedspaceId: UUID,
  val actions: List<Cas3BedspaceArchiveAction>,
)
