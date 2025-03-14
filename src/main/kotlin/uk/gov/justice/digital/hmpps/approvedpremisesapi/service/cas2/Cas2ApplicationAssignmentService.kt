package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2ApplicationAssignmentService(
  private val cas2ApplicationService: Cas2ApplicationService,
  private val cas2ApplicationAssignmentRepository: Cas2ApplicationAssignmentRepository,
) {

  @Transactional
  fun createApplicationAssignment(nomsNumber: String, prisonCode: String, allocatedPomUserId: UUID?) {
    cas2ApplicationService.findMostRecentApplication(nomsNumber)?.let {
      val location = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = it,
        prisonCode = prisonCode,
        allocatedPomUserId = allocatedPomUserId,
        createdAt = OffsetDateTime.now(),
      )
      cas2ApplicationAssignmentRepository.save(location)
    }
  }
}
