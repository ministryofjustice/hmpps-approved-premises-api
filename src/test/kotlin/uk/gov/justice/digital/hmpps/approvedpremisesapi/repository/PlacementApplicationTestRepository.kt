package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.util.UUID

@Repository
interface PlacementApplicationTestRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  fun findByApplicationAndReallocatedAtNull(application: ApplicationEntity): PlacementApplicationEntity
}
