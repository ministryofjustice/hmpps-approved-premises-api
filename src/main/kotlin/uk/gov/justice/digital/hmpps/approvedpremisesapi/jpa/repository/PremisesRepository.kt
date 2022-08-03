package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.util.UUID

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID>
