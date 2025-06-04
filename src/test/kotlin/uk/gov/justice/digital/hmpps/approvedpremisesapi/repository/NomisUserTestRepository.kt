package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.NomisUserEntity
import java.util.UUID

@Repository
interface NomisUserTestRepository : JpaRepository<NomisUserEntity, UUID> {
  fun findByNomisUsername(nomisUsername: String): NomisUserEntity?
}
