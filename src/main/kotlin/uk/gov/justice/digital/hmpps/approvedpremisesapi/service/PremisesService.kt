package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import java.util.UUID

@Service
class PremisesService(
  private val premisesRepository: PremisesRepository,
) {
  fun getPremises(premisesId: UUID): PremisesEntity? = premisesRepository.findByIdOrNull(premisesId)

  fun getBedCount(premises: PremisesEntity): Int = premisesRepository.getBedCount(premises)
}
