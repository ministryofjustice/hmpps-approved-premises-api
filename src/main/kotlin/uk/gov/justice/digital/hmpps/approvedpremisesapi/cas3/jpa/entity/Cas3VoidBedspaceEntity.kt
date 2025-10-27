package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3VoidBedspacesRepository : JpaRepository<Cas3VoidBedspaceEntity, UUID> {
  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb WHERE vb.premises.id = :premisesId AND vb.startDate <= :endDate AND vb.endDate >= :startDate")
  fun findAllByPremisesIdAndOverlappingDate(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb WHERE vb.bed.id IN :voidBedspaceIds")
  fun findByBedIds(voidBedspaceIds: List<UUID>): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb 
    LEFT JOIN vb.cancellation c 
    WHERE vb.bed.id = :bedId AND 
          vb.startDate <= :endDate AND 
          vb.endDate >= :startDate AND 
          (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR vb.id != :thisEntityId) AND 
          c is NULL
  """,
  )
  fun findByBedspaceIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb 
    WHERE vb.bedspace.id = :bedspaceId AND 
          vb.startDate <= :endDate AND 
          vb.endDate >= :startDate AND 
          (CAST(:bookingId as org.hibernate.type.UUIDCharType) IS NULL OR vb.id != :bookingId) AND 
          vb.cancellationDate is NULL
  """,
  )
  fun findByBedspaceIdAndOverlappingDateV2(bedspaceId: UUID, startDate: LocalDate, endDate: LocalDate, bookingId: UUID?): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    WHERE vb.bed.id = :bedspaceId
    AND vb.endDate > :bedspaceEndDate 
    AND vb.cancellation is NULL
  """,
  )
  fun findOverlappingBedspaceEndDate(bedspaceId: UUID, bedspaceEndDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    WHERE vb.bedspace.id = :bedspaceId
    AND vb.endDate > :bedspaceEndDate 
    AND vb.cancellationDate IS NULL
  """,
  )
  fun findOverlappingBedspaceEndDateV2(bedspaceId: UUID, bedspaceEndDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    JOIN PremisesEntity p ON vb.premises.id = p.id
    WHERE p.id = :premisesId
    AND vb.endDate > :endDate 
    AND vb.cancellation is NULL
  """,
  )
  fun findOverlappingBedspaceEndDateByPremisesId(premisesId: UUID, endDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query(
    """
    SELECT vb 
    FROM Cas3VoidBedspaceEntity vb
    WHERE vb.bedspace.premises.id = :premisesId
    AND vb.endDate > :endDate 
    AND vb.cancellationDate IS NULL
  """,
  )
  fun findOverlappingBedspaceEndDateByPremisesIdV2(premisesId: UUID, endDate: LocalDate): List<Cas3VoidBedspaceEntity>

  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb LEFT JOIN vb.cancellation c WHERE vb.startDate <= :endDate AND vb.endDate >= :startDate AND vb.bed = :bed AND c is NULL")
  fun findAllByOverlappingDateForBedspace(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<Cas3VoidBedspaceEntity>

  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb WHERE vb.startDate <= :endDate AND vb.endDate >= :startDate AND vb.bedspace = :bedspace AND vb.cancellationDate is NULL ORDER BY vb.startDate DESC")
  fun findAllByOverlappingDateForBedspace(startDate: LocalDate, endDate: LocalDate, bedspace: Cas3BedspacesEntity): List<Cas3VoidBedspaceEntity>

  @Query("SELECT vb FROM Cas3VoidBedspaceEntity vb LEFT JOIN vb.cancellation c WHERE vb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<Cas3VoidBedspaceEntity>

  @Query(
    "select vb from Cas3VoidBedspaceEntity vb where vb.bedspace.premises.id = :premisesId " +
      "and vb.cancellationDate is null",
  )
  fun findActiveVoidBedspacesByPremisesId(premisesId: UUID): List<Cas3VoidBedspaceEntity>

  @Query("""select vb from Cas3VoidBedspaceEntity vb where vb.id = :voidBedspaceId and vb.bedspace.premises.id = :premisesId""")
  fun findVoidBedspace(premisesId: UUID, voidBedspaceId: UUID): Cas3VoidBedspaceEntity?

  @Query(
    """select vb from Cas3VoidBedspaceEntity vb 
      where vb.id = :voidBedspaceId and vb.bedspace.id = :bedspaceId and vb.bedspace.premises.id = :premisesId 
      and vb.cancellationDate is null""",
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
  var premises: PremisesEntity?,
  @OneToOne(mappedBy = "voidBedspace")
  var cancellation: Cas3VoidBedspaceCancellationEntity?,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bed: BedEntity?,

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
