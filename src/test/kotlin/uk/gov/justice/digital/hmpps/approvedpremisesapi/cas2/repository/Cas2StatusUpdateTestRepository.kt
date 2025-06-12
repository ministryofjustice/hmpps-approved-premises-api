package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import java.util.UUID

@Repository
interface Cas2StatusUpdateTestRepository : JpaRepository<Cas2StatusUpdateEntity, UUID>
