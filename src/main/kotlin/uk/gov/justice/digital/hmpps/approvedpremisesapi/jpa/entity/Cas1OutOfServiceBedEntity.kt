package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1OutOfServiceBedRepository : JpaRepository<Cas1OutOfServiceBedEntity, UUID> {

  companion object {
    const val OOSB_QUERY = """
    FROM cas1_out_of_service_beds oosb
    INNER JOIN (
      SELECT DISTINCT ON (out_of_service_bed_id)
         out_of_service_bed_id,
         start_date,
         end_date,
         created_at as max_created_at
      FROM cas1_out_of_service_bed_revisions
      ORDER BY out_of_service_bed_id, created_at DESC      
    ) dd
    ON oosb.id = dd.out_of_service_bed_id
    LEFT JOIN cas1_out_of_service_bed_revisions d
    ON dd.out_of_service_bed_id = d.out_of_service_bed_id
    AND dd.max_created_at = d.created_at
    LEFT JOIN premises p
    ON oosb.premises_id = p.id
    LEFT JOIN probation_regions pr
    ON p.probation_region_id = pr.id
    LEFT JOIN ap_areas apa
    ON pr.ap_area_id = apa.id
    LEFT JOIN beds b
    ON oosb.bed_id = b.id
    LEFT JOIN rooms r
    ON b.room_id = r.id
    LEFT JOIN cas1_out_of_service_bed_reasons oosr
    ON d.out_of_service_bed_reason_id = oosr.id
    LEFT JOIN cas1_out_of_service_bed_cancellations oosb_cancellations
    ON oosb.id = oosb_cancellations.out_of_service_bed_id
    WHERE
      (CAST(:premisesId AS UUID) IS NULL OR oosb.premises_id = :premisesId) AND
      (CAST(:apAreaId AS UUID) IS NULL OR apa.id = :apAreaId) AND 
      (FALSE = :excludePast OR dd.end_date >= CURRENT_DATE) AND
      (FALSE = :excludeCurrent OR CURRENT_DATE NOT BETWEEN dd.start_date AND dd.end_date) AND
      (FALSE = :excludeFuture OR dd.start_date <= CURRENT_DATE) AND 
      (oosb_cancellations IS NULL)
    """
  }

  @Query(
    """
    SELECT CAST(oosb.id AS TEXT)
    $OOSB_QUERY
    """,
    countQuery = """
    SELECT COUNT(1) as count
    $OOSB_QUERY
    """,
    nativeQuery = true,
  )
  fun findOutOfServiceBedIds(
    premisesId: UUID?,
    apAreaId: UUID?,
    excludePast: Boolean,
    excludeCurrent: Boolean,
    excludeFuture: Boolean,
    pageable: Pageable?,
  ): Page<String>

  @Query("SELECT oosb FROM Cas1OutOfServiceBedEntity oosb LEFT JOIN oosb.cancellation c WHERE c is NULL")
  fun findAllActive(): List<Cas1OutOfServiceBedEntity>

  @Query("SELECT oosb FROM Cas1OutOfServiceBedEntity oosb LEFT JOIN oosb.cancellation c WHERE oosb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<Cas1OutOfServiceBedEntity>

  @Query(
    """
    SELECT
      CAST(oosb.id AS TEXT)
    FROM cas1_out_of_service_beds oosb
    INNER JOIN (
      SELECT DISTINCT ON (out_of_service_bed_id)
         out_of_service_bed_id,
         start_date,
         end_date,
         created_at as max_created_at
      FROM cas1_out_of_service_bed_revisions
      ORDER BY out_of_service_bed_id, created_at DESC      
    ) latest_revision
    ON oosb.id = latest_revision.out_of_service_bed_id
    LEFT JOIN cas1_out_of_service_bed_cancellations c
    ON oosb.id = c.out_of_service_bed_id
    LEFT JOIN beds b
    ON oosb.bed_id = b.id
    WHERE
      b.id = :bedId AND
      (CAST(:thisEntityId AS UUID) IS NULL OR oosb.id != :thisEntityId) AND
      latest_revision.start_date <= :endDate AND
      latest_revision.end_date >= :startDate AND
      c IS NULL
    """,
    nativeQuery = true,
  )
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<String>
}

@Entity
@Table(name = "cas1_out_of_service_beds")
data class Cas1OutOfServiceBedEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: ApprovedPremisesEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  val bed: BedEntity,
  val createdAt: OffsetDateTime,
  @OneToOne(mappedBy = "outOfServiceBed")
  var cancellation: Cas1OutOfServiceBedCancellationEntity?,
  @OneToMany(mappedBy = "outOfServiceBed")
  var revisionHistory: MutableList<Cas1OutOfServiceBedRevisionEntity>,
) {
  val latestRevision: Cas1OutOfServiceBedRevisionEntity
    get() = revisionHistory.maxBy { it.createdAt }

  val reason: Cas1OutOfServiceBedReasonEntity
    get() = latestRevision.reason

  /**
   * inclusive
   */
  val startDate
    get() = latestRevision.startDate

  /**
   * inclusive
   */
  val endDate
    get() = latestRevision.endDate

  val referenceNumber
    get() = latestRevision.referenceNumber

  val notes
    get() = latestRevision.notes

  fun isApplicable(now: LocalDate, candidate: BedEntity): Boolean {
    return bed.id == candidate.id &&
      cancellation == null &&
      (!now.isBefore(startDate) && !now.isAfter(endDate))
  }
}
