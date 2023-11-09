package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import java.util.UUID

@Service("Cas2StatusUpdateService")
class StatusUpdateService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
) {

  fun isValidStatus(statusUpdate: Cas2ApplicationStatusUpdate): Boolean {
    return findStatusByName(statusUpdate.newStatus) != null
  }

  fun create(
    applicationId: UUID,
    statusUpdate: Cas2ApplicationStatusUpdate,
    assessor: ExternalUserEntity,
  ) = validated<Cas2StatusUpdateEntity> {
    val application = applicationRepository.findSubmittedApplicationById(applicationId)
      ?: return "$.applicationId" hasSingleValidationError "doesNotExist"

    val status = findStatusByName(statusUpdate.newStatus)
      ?: return "$.statusUpdate.newStatus" hasSingleValidationError "doesNotExist"

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val createdStatusUpdate = statusUpdateRepository.save(
      Cas2StatusUpdateEntity(
        id = UUID.randomUUID(),
        application = application,
        assessor = assessor,
        statusId = status.id,
        description = status.description,
        label = status.label,
      ),
    )

    return success(createdStatusUpdate)
  }

  private fun findStatusByName(statusName: String): Cas2ApplicationStatus? {
    return Cas2ApplicationStatusSeeding.statusList()
      .find { status -> status.name == statusName }
  }
}
