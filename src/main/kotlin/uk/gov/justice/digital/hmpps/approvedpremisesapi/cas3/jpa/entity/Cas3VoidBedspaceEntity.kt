package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3VoidBedspacesRepository : JpaRepository<Cas3VoidBedspaceEntity, UUID> {
  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb 
    WHERE vb.bedspace.id = :bedspaceId AND 
          vb.startDate <= :endDate AND 
          vb.endDate >= :startDate AND 
          (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR vb.id != :thisEntityId) AND 
          vb.cancellationDate is NULL
  """,
  )
  fun findByBedspaceIdAndOverlappingDate(bedspaceId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    WHERE vb.bedspace.id = :bedspaceId
    AND vb.endDate > :bedspaceEndDate 
    AND vb.cancellationDate IS NULL
  """,
  )
  fun findOverlappingBedspaceEndDate(bedspaceId: UUID, bedspaceEndDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    WHERE vb.bedspace.premises.id = :premisesId
    AND vb.endDate > :endDate 
    AND vb.cancellationDate is NULL
  """,
  )
  fun findOverlappingBedspaceEndDateByPremisesId(premisesId: UUID, endDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb WHERE vb.startDate <= :endDate AND vb.endDate >= :startDate AND vb.bedspace = :bedspace AND vb.cancellationDate is NULL ORDER BY vb.startDate DESC")
  fun findAllByOverlappingDateForBedspace(startDate: LocalDate, endDate: LocalDate, bedspace: Cas3BedspacesEntity): List<Cas3VoidBedspaceEntity>

  @Query(
    """
      SELECT vb FROM Cas3VoidBedspaceEntity vb 
      JOIN FETCH vb.reason 
      WHERE vb.startDate <= :endDate 
        AND vb.endDate >= :startDate 
        AND vb.bedspace.id IN :bedspaceIds 
        AND vb.cancellationDate is NULL 
      ORDER BY vb.startDate DESC
  """,
  )
  fun findAllByOverlappingDateForBedspaceIds(startDate: LocalDate, endDate: LocalDate, bedspaceIds: List<UUID>): List<Cas3VoidBedspaceEntity>

  @Query(
    "select vb from Cas3VoidBedspaceEntity vb where vb.bedspace.premises.id = :premisesId " +
      "and vb.cancellationDate is null",
  )
  fun findActiveVoidBedspacesByPremisesId(premisesId: UUID): List<Cas3VoidBedspaceEntity>

  @Query(
    """select vb from Cas3VoidBedspaceEntity vb 
      where vb.id = :voidBedspaceId and vb.bedspace.id = :bedspaceId and vb.bedspace.premises.id = :premisesId""",
  )
  fun findVoidBedspace(premisesId: UUID, bedspaceId: UUID, voidBedspaceId: UUID): Cas3VoidBedspaceEntity?
}

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_void_bedspaces")
class Cas3VoidBedspaceEntity(
  @Id
  val id: UUID,
  var startDate: LocalDate,
  var endDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "cas3_void_bedspace_reason_id")
  var reason: Cas3VoidBedspaceReasonEntity,
  var referenceNumber: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: Cas3PremisesEntity?,
  @ManyToOne
  @JoinColumn(name = "bedspace_id")
  var bedspace: Cas3BedspacesEntity?,
  var cancellationDate: OffsetDateTime?,
  var cancellationNotes: String?,
  @Enumerated(EnumType.STRING)
  var costCentre: Cas3CostCentre?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3VoidBedspaceEntity) return false

    if (id != other.id) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false
    if (reason != other.reason) return false
    if (referenceNumber != other.referenceNumber) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, startDate, endDate, reason, referenceNumber, notes)

  override fun toString() = "Cas3VoidBedspaceEntity:$id"
}
