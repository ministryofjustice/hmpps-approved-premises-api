package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DomainBedSummary
import java.util.UUID

@Service
class Cas1BedService(
  private val cas1BedRepository: Cas1BedRepository,
  private val cas1CharacteristicRepository: Cas1CharacteristicRepository,
) {

  fun getBedAndRoomCharacteristics(id: UUID): CasResult<Pair<Cas1DomainBedSummary, List<Cas1CharacteristicEntity>>> {
    val bedDetail = cas1BedRepository.getDetailById(id) ?: return CasResult.NotFound("Bed", id.toString())
    val characteristics = cas1CharacteristicRepository.findAllForRoomId(bedDetail.roomId)

    return CasResult.Success(
      Pair(bedDetail, characteristics),
    )
  }
}
