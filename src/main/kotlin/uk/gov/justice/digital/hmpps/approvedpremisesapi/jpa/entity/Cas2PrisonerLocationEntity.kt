package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2OffenderEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2PrisonerLocationRepository : JpaRepository<Cas2PrisonerLocationEntity, UUID>

@Entity
@Table(name = "cas_2_prisoner_locations")
data class Cas2PrisonerLocationEntity(
  @Id
  val id: UUID,
  val prisonCode: String,
  val allocatedPomUserId: UUID?,
  val createdAt: OffsetDateTime,

  @ManyToOne
  @JoinColumn(name = "offender_id", nullable = false)
  val offender: Cas2OffenderEntity,
)
