package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.util.UUID

@Service
class Cas1BedService(
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) {

  fun getBedAndRoomCharacteristics(id: UUID): AuthorisableActionResult<Pair<DomainBedSummary, List<CharacteristicEntity>>> {
    val bedDetail = bedRepository.getDetailById(id) ?: return AuthorisableActionResult.NotFound()
    val characteristics = characteristicRepository.findAllForRoomId(bedDetail.roomId)

    return AuthorisableActionResult.Success(
      Pair(bedDetail, characteristics),
    )
  }
}
