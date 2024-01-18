package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface LostBedsRepository : JpaRepository<LostBedsEntity, UUID> {
  @Query("SELECT lb FROM LostBedsEntity lb WHERE lb.premises.id = :premisesId AND lb.startDate <= :endDate AND lb.endDate >= :startDate")
  fun findAllByPremisesIdAndOverlappingDate(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): List<LostBedsEntity>

  @Query("SELECT MAX(lb.endDate) FROM LostBedsEntity lb WHERE lb.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?

  @Query("SELECT lb FROM LostBedsEntity lb WHERE lb.bed.id IN :lostBedIds")
  fun findByBedIds(lostBedIds: List<UUID>): List<LostBedsEntity>

  @Query(
    """
    SELECT lb 
    FROM LostBedsEntity lb 
    LEFT JOIN lb.cancellation c 
    WHERE lb.bed.id = :bedId AND 
          lb.startDate <= :endDate AND 
          lb.endDate >= :startDate AND 
          (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR lb.id != :thisEntityId) AND 
          c is NULL
  """,
  )
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<LostBedsEntity>

  @Query("SELECT lb FROM LostBedsEntity lb LEFT JOIN lb.cancellation c WHERE lb.startDate <= :endDate AND lb.endDate >= :startDate AND lb.bed = :bed AND c is NULL")
  fun findAllByOverlappingDateForBed(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<LostBedsEntity>

  @Query("SELECT lb FROM LostBedsEntity lb LEFT JOIN lb.cancellation c WHERE lb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<LostBedsEntity>
}

@Entity
@Table(name = "lost_beds")
class LostBedsEntity(
  @Id
  val id: UUID,
  var startDate: LocalDate,
  var endDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "lost_bed_reason_id")
  var reason: LostBedReasonEntity,
  var referenceNumber: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity,
  @OneToOne(mappedBy = "lostBed")
  var cancellation: LostBedCancellationEntity?,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bed: BedEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LostBedsEntity) return false

    if (id != other.id) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false
    if (reason != other.reason) return false
    if (referenceNumber != other.referenceNumber) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, startDate, endDate, reason, referenceNumber, notes)

  override fun toString() = "LostBedsEntity:$id"
}
