package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1PremisesLocalRestrictionRepository : JpaRepository<Cas1PremisesLocalRestrictionEntity, UUID> {
  fun findAllByApprovedPremisesId(approvedPremisesId: UUID): List<Cas1PremisesLocalRestrictionEntity>

  fun findAllByApprovedPremisesIdAndArchivedFalseOrderByCreatedAtDesc(approvedPremisesId: UUID): List<Cas1PremisesLocalRestrictionEntity>

  @Query(
    """
    SELECT r.description
    FROM Cas1PremisesLocalRestrictionEntity r
    WHERE r.approvedPremisesId = :premisesId
    AND r.archived = false
    order by r.createdAt DESC
  """,
  )
  fun findAllActiveRestrictionDescriptionsByPremisesId(premisesId: UUID): List<String>
}

@Entity
@Table(name = "cas1_premises_local_restrictions")
data class Cas1PremisesLocalRestrictionEntity(
  @Id
  val id: UUID,
  val description: String,
  val createdAt: OffsetDateTime,
  val createdByUserId: UUID,
  val approvedPremisesId: UUID,
  var archived: Boolean = false,
)
