package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2PrisonerLocationRepository : JpaRepository<Cas2PrisonerLocationEntity, UUID> {

  @Query("SELECT p FROM Cas2PrisonerLocationEntity p WHERE p.application.id = :applicationId and p.endDate is NULL")
  fun findPrisonerLocation(applicationId: UUID): Cas2PrisonerLocationEntity?
}

@Entity
@Table(name = "cas_2_prisoner_locations")
data class Cas2PrisonerLocationEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,
  val prisonCode: String,
  val staffId: UUID?,
  val occurredAt: OffsetDateTime,
  val endDate: OffsetDateTime?,
)
