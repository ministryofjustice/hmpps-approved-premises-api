package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.KeyWorkerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.KeyWorkerRepository
import java.util.UUID

@Service
class KeyWorkerService(private val keyWorkerRepository: KeyWorkerRepository) {
  fun getKeyWorker(id: UUID): KeyWorkerEntity? = keyWorkerRepository.findByIdOrNull(id)
}
