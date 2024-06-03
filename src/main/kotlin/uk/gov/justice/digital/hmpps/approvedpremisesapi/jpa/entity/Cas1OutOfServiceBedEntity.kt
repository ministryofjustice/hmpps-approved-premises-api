package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface Cas1OutOfServiceBedRepository : JpaRepository<Cas1OutOfServiceBedEntity, UUID> {
  @Query("SELECT oosb FROM Cas1OutOfServiceBedEntity oosb LEFT JOIN oosb.cancellation c WHERE oosb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<Cas1OutOfServiceBedEntity>

  @Query(
    """
    SELECT oosb 
    FROM Cas1OutOfServiceBedEntity oosb 
    LEFT JOIN oosb.cancellation c 
    WHERE oosb.bed.id = :bedId AND 
          oosb.startDate <= :endDate AND 
          oosb.endDate >= :startDate AND 
          (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR oosb.id != :thisEntityId) AND 
          c is NULL
    """,
  )
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<Cas1OutOfServiceBedEntity>
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
  @JoinColumn(name = "out_of_service_bed_reason_id")
  var reason: Cas1OutOfServiceBedReasonEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  val bed: BedEntity,
  val createdAt: OffsetDateTime,
  var startDate: LocalDate,
  var endDate: LocalDate,
  var referenceNumber: String?,
  var notes: String?,
  @OneToOne(mappedBy = "outOfServiceBed")
  var cancellation: Cas1OutOfServiceBedCancellationEntity?,
)
