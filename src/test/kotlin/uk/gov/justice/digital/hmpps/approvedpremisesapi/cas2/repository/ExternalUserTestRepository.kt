package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import java.util.UUID

@Repository
interface ExternalUserTestRepository : JpaRepository<ExternalUserEntity, UUID> {
  fun findByUsername(username: String): ExternalUserEntity?
}
