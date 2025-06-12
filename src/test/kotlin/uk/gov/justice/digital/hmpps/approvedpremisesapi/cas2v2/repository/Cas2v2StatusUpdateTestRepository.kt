package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import java.util.UUID

@Repository
interface Cas2v2StatusUpdateTestRepository : JpaRepository<Cas2v2StatusUpdateEntity, UUID>
