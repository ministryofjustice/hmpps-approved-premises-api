package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesRepository
import java.util.*

@Service
class Cas3PremisesService(private val cas3PremisesRepository: Cas3PremisesRepository) {
  fun getPremises(premisesId: UUID): Cas3PremisesEntity? = cas3PremisesRepository.findByIdOrNull(premisesId)

}