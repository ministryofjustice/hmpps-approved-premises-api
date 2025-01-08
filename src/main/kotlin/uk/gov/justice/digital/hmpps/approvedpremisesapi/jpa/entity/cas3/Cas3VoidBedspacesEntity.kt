package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

@Repository
interface Cas3VoidBedspacesRepository : JpaRepository<Cas3VoidBedspacesEntity, UUID> {
  @Query("SELECT lb FROM Cas3VoidBedspacesEntity lb WHERE lb.premises.id = :premisesId AND lb.startDate <= :endDate AND lb.endDate >= :startDate")
  fun findAllByPremisesIdAndOverlappingDate(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): List<Cas3VoidBedspacesEntity>

  @Query("SELECT MAX(lb.endDate) FROM Cas3VoidBedspacesEntity lb WHERE lb.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?

  @Query("SELECT lb FROM Cas3VoidBedspacesEntity lb WHERE lb.bed.id IN :voidBedspaceIds")
  fun findByBedIds(voidBedspaceIds: List<UUID>): List<Cas3VoidBedspacesEntity>

  @Query(
    """
    SELECT lb 
    FROM Cas3VoidBedspacesEntity lb 
    LEFT JOIN lb.cancellation c 
    WHERE lb.bed.id = :bedId AND 
          lb.startDate <= :endDate AND 
          lb.endDate >= :startDate AND 
          (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR lb.id != :thisEntityId) AND 
          c is NULL
  """,
  )
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<Cas3VoidBedspacesEntity>

  @Query("SELECT lb FROM Cas3VoidBedspacesEntity lb LEFT JOIN lb.cancellation c WHERE lb.startDate <= :endDate AND lb.endDate >= :startDate AND lb.bed = :bed AND c is NULL")
  fun findAllByOverlappingDateForBed(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<Cas3VoidBedspacesEntity>

  @Query("SELECT lb FROM Cas3VoidBedspacesEntity lb LEFT JOIN lb.cancellation c WHERE lb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<Cas3VoidBedspacesEntity>
}

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_void_bedspaces")
class Cas3VoidBedspacesEntity(
  @Id
  val id: UUID,
  var startDate: LocalDate,
  var endDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "lost_bed_reason_id")
  var reason: Cas3VoidBedspaceReasonEntity,
  var referenceNumber: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity,
  @OneToOne(mappedBy = "voidBedspace")
  var cancellation: Cas3VoidBedspaceCancellationEntity?,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bed: BedEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3VoidBedspacesEntity) return false

    if (id != other.id) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false
    if (reason != other.reason) return false
    if (referenceNumber != other.referenceNumber) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, startDate, endDate, reason, referenceNumber, notes)

  override fun toString() = "Cas3VoidBedspacesEntity:$id"
}