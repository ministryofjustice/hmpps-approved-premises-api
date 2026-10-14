package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

object Cas2ApplicationStatusSeeding {

  fun activeStatuses(): List<Cas2AssessmentStatus> = Cas2AssessmentStatus.entries.filter { it.isActive }
  fun statusById(id: UUID) = Cas2AssessmentStatus.entries.firstOrNull { it.id == id }

  fun statusDetailsByStatus(status: Cas2AssessmentStatus, service: ServiceName = ServiceName.cas2v2): List<Cas2AssessmentStatusDetail>? = Cas2AssessmentStatusDetail.entries
    .filter {
      it.applicableToServices.contains(service) &&
        it.parentStatus == status
    }.takeIf { it.isNotEmpty() && status.hasStatusDetails }
}
