package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.util.UUID

@Service
class Cas1PremisesService(
  val premisesRepository: ApprovedPremisesRepository,
  val premisesService: PremisesService,
  val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
) {
  fun getPremisesSummary(premisesId: UUID): CasResult<Cas1PremisesSummaryInfo> {
    val premise = premisesRepository.findByIdOrNull(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    val bedCount = premisesService.getBedCount(premise)
    val outOfServiceBedsCount = cas1OutOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(premisesId)

    return CasResult.Success(
      Cas1PremisesSummaryInfo(
        entity = premise,
        bedCount = bedCount,
        availableBeds = bedCount - outOfServiceBedsCount,
        outOfServiceBeds = outOfServiceBedsCount,
      ),
    )
  }

  fun getPremises(gender: ApprovedPremisesGender?) = premisesRepository.findForSummaries(gender)

  fun findPremiseById(id: UUID) = premisesRepository.findByIdOrNull(id)

  data class Cas1PremisesSummaryInfo(
    val entity: ApprovedPremisesEntity,
    val bedCount: Int,
    val availableBeds: Int,
    val outOfServiceBeds: Int,
  )
}
