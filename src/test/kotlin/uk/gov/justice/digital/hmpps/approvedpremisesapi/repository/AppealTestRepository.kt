package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import java.util.UUID

@Repository
interface AppealTestRepository : JpaRepository<AppealEntity, UUID> {
  fun findByApplication_Id(applicationId: UUID): AppealEntity?
}
