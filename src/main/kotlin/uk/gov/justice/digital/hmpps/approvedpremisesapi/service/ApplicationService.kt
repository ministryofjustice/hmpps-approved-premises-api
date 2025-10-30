package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.util.UUID

@Service
@Suppress(
  "LongParameterList",
  "TooManyFunctions",
  "ReturnCount",
  "ThrowsCount",
  "TooGenericExceptionThrown",
)
class ApplicationService(
  private val applicationRepository: ApplicationRepository,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

  fun updateApprovedPremisesApplicationStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus) {
    applicationRepository.updateStatus(applicationId, status)
  }
}
