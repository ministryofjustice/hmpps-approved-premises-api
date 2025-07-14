package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity
import java.util.UUID

@Repository
interface Cas3v2ConfirmationTestRepository : JpaRepository<Cas3v2ConfirmationEntity, UUID>
