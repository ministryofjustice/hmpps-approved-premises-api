package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.repository.PremisesRepository

@Service
class PremisesService(private val premisesRepository: PremisesRepository) {
  fun getAllPremises(): List<PremisesEntity> = premisesRepository.findAll()
}
