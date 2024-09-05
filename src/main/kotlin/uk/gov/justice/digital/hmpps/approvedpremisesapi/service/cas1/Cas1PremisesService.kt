package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.util.UUID

@Service
class Cas1PremisesService(
  val premisesRepository: PremisesRepository,
  val premisesService: PremisesService,
  val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
) {
  fun getPremisesSummary(premisesId: UUID): CasResult<Cas1PremisesSummary> {
    val premise = premisesRepository.findByIdOrNull(premisesId)
    if (premise !is ApprovedPremisesEntity) return CasResult.NotFound("premises", premisesId.toString())

    val bedCount = premisesService.getBedCount(premise)
    val outOfServiceBedsCount = cas1OutOfServiceBedService.getActiveOutOfServiceBedsCountForPremisesId(premisesId)

    return CasResult.Success(
      Cas1PremisesSummary(
        id = premisesId,
        name = premise.name,
        apCode = premise.apCode,
        postcode = premise.postcode,
        bedCount = bedCount,
        availableBeds = bedCount - outOfServiceBedsCount,
        outOfServiceBeds = outOfServiceBedsCount,
      ),
    )
  }
}
