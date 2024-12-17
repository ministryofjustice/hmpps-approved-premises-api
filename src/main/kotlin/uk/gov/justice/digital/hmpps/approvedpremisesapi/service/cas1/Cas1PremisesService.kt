package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1PremisesService(
  val premisesRepository: ApprovedPremisesRepository,
  val premisesService: PremisesService,
  val outOfServiceBedService: Cas1OutOfServiceBedService,
  val spacePlanningService: SpacePlanningService,
) {
  fun getPremisesSummary(premisesId: UUID): CasResult<Cas1PremisesSummaryInfo> {
    val premise = premisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    val bedCount = premisesService.getBedCount(premise)
    val outOfServiceBedsCount = outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(premisesId)

    return CasResult.Success(
      Cas1PremisesSummaryInfo(
        entity = premise,
        bedCount = bedCount,
        availableBeds = bedCount - outOfServiceBedsCount,
        outOfServiceBeds = outOfServiceBedsCount,
      ),
    )
  }

  fun getPremises(gender: ApprovedPremisesGender?, apAreaId: UUID?) = premisesRepository.findForSummaries(gender, apAreaId)

  fun findPremiseById(id: UUID) = premisesRepository.findByIdOrNull(id)

  fun getPremiseCapacity(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeSpaceBookingId: UUID?,
  ): CasResult<SpacePlanningService.PremiseCapacitySummary> {
    val premises = premisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    if (startDate.isAfter(endDate)) {
      return CasResult.GeneralValidationError("Start Date $startDate should be before End Date $endDate")
    }

    return CasResult.Success(
      spacePlanningService.capacity(
        premises = premises,
        range = DateRange(startDate, endDate),
        excludeSpaceBookingId = excludeSpaceBookingId,
      ),
    )
  }

  data class Cas1PremisesSummaryInfo(
    val entity: ApprovedPremisesEntity,
    val bedCount: Int,
    val availableBeds: Int,
    val outOfServiceBeds: Int,
  )
}
