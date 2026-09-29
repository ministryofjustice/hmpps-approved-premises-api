package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

object Cas2ApplicationStatusSeeding {

  fun statusList(service: ServiceName): List<Cas2PersistedApplicationStatus> = Cas2AssessmentStatus.entries
    .map { status ->
      when {
        status.hasStatusDetails -> {
          val applicableDetails = Cas2AssessmentStatusDetail.entries
            .filter {
              it.applicableToServices.contains(service) &&
                it.parentStatus == status
            }
            .takeIf { it.isNotEmpty() }

          Cas2PersistedApplicationStatus(
            status = status,
            statusDetails = applicableDetails,
          )
        }
        else -> Cas2PersistedApplicationStatus(
          status = status,
        )
      }
    }
}
