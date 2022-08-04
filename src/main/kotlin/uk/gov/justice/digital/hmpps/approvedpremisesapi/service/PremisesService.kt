package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.repository.PremisesRepository
import java.util.UUID

@Service
class PremisesService(private val premisesRepository: PremisesRepository) {
  fun getAllPremises(): List<PremisesEntity> = premisesRepository.findAll()
  fun getPremises(premisesId: UUID): PremisesEntity? = premisesRepository.findByIdOrNull(premisesId)
}
