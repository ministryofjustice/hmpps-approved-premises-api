package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.util.UUID

@Service
class Cas1PlacementApplicationValidationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
) {

  @SuppressWarnings("ReturnCount")
  fun validateDecision(
    id: UUID,
    decisionEnvelope: PlacementApplicationDecisionEnvelope,
    user: UserEntity,
  ): CasResult<ValidatedDecision> {
    val placementApplicationEntity =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
        entityType = "PlacementApplication",
        id = id.toString(),
      )

    if (placementApplicationEntity.allocatedToUser?.id != user.id) {
      return CasResult.Unauthorised()
    }

    if (placementApplicationEntity.decision != null) {
      return CasResult.GeneralValidationError("This application has already had a decision set")
    }

    val decisionDto = decisionEnvelope.decision
    if (decisionDto == PlacementApplicationDecisionDto.withdraw || decisionDto == PlacementApplicationDecisionDto.withdrawnByPp) {
      return CasResult.GeneralValidationError("Decision $decisionDto is not supported")
    }

    return CasResult.Success(
      ValidatedDecision(
        placementApplicationEntity,
      ),
    )
  }

  data class ValidatedDecision(
    val placementApplication: PlacementApplicationEntity,
  )
}
