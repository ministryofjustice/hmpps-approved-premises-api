package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1AuthorisedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
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

    if(decisionDto == PlacementApplicationDecisionDto.rejected) {
      // TODO: test authorisedPlacementPeriod on response
      return CasResult.Success(
        ValidatedDecision(
          placementApplication = placementApplicationEntity,
          authorisedPlacementPeriod = null,
        ),
      )

    } else {
      val acceptance = decisionEnvelope.acceptance

      // TODO: test
      if(placementApplicationEntity.requestedDuration == null && acceptance == null) {
        return CasResult.GeneralValidationError("Acceptance with a duration is required when requested duration is null")
      }

      val authorisedPlacementPeriod = acceptance?.authorisedPlacementPeriod
          ?: Cas1AuthorisedPlacementPeriod(
            arrival = placementApplicationEntity.expectedArrival!!,
            arrivalFlexible = placementApplicationEntity.expectedArrivalFlexible,
            duration = placementApplicationEntity.requestedDuration!!,
          )

      // TODO: test
      if(authorisedPlacementPeriod.arrival != placementApplicationEntity.expectedArrival) {
        return CasResult.GeneralValidationError("Cannot change arrival date on acceptance")
      }

      // TODO: test
      if(authorisedPlacementPeriod.arrivalFlexible != placementApplicationEntity.expectedArrivalFlexible) {
        return CasResult.GeneralValidationError("Cannot change if arrival is flexible on acceptance")
      }

      // TODO: test
      if(placementApplicationEntity.requestedDuration != null && authorisedPlacementPeriod.duration != placementApplicationEntity.requestedDuration) {
        return CasResult.GeneralValidationError("Cannot only specify a different duration when requested duration is not defined")
      }

      // TODO: test authorisedPlacementPeriod on response
      return CasResult.Success(
        ValidatedDecision(
          placementApplicationEntity,
          authorisedPlacementPeriod = authorisedPlacementPeriod,
        ),
      )

    }

  }

  data class ValidatedDecision(
    val placementApplication: PlacementApplicationEntity,
    val authorisedPlacementPeriod: Cas1AuthorisedPlacementPeriod?
  )

}
