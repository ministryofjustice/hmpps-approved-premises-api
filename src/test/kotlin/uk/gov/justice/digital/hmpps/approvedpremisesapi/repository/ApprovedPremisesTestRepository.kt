package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import java.util.UUID

@Repository
interface ApprovedPremisesTestRepository : JpaRepository<ApprovedPremisesEntity, UUID> {
  fun findByApCode(name: String): ApprovedPremisesEntity?
  fun findByQCode(name: String): ApprovedPremisesEntity?
}
