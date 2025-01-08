package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceReasonEntity
import java.util.UUID

@Repository
interface Cas3VoidBedspaceReasonTestRepository : JpaRepository<Cas3VoidBedspaceReasonEntity, UUID>