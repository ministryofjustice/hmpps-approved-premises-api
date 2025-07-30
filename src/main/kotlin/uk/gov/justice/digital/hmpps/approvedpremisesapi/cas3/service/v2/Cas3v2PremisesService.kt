package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import java.util.UUID

@Service
class Cas3v2PremisesService(private val cas3PremisesRepository: Cas3PremisesRepository) {
  fun getPremises(premisesId: UUID): Cas3PremisesEntity? = cas3PremisesRepository.findByIdOrNull(premisesId)
}
