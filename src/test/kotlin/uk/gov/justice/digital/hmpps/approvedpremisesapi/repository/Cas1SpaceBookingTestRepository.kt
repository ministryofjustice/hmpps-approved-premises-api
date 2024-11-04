package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import java.util.UUID

@Service
interface Cas1SpaceBookingTestRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  fun findByPremisesId(premisesId: UUID): List<Cas1SpaceBookingEntity>
}
