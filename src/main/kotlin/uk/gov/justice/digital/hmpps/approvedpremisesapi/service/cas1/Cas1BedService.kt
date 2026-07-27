package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DomainBedSummary
import java.util.UUID

@Service
class Cas1BedService(
  private val cas1BedRepository: Cas1BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) {

  fun getBedAndRoomCharacteristics(id: UUID): CasResult<Pair<Cas1DomainBedSummary, List<CharacteristicEntity>>> {
    val bedDetail = cas1BedRepository.getDetailById(id) ?: return CasResult.NotFound("Bed", id.toString())
    val characteristics = characteristicRepository.findAllForRoomId(bedDetail.roomId)

    return CasResult.Success(
      Pair(bedDetail, characteristics),
    )
  }
}
