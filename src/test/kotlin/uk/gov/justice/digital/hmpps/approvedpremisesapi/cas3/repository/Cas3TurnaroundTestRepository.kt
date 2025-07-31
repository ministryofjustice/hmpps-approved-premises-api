package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3TurnaroundEntity
import java.util.UUID

@Repository
interface Cas3TurnaroundTestRepository : JpaRepository<Cas3TurnaroundEntity, UUID>
