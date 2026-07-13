package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_bedspaces")
@Inheritance(strategy = InheritanceType.JOINED)
data class Cas3BedspacesEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: Cas3PremisesEntity,

  var reference: String,
  var notes: String?,
  val startDate: LocalDate,
  var endDate: LocalDate?,
  var createdAt: OffsetDateTime,
  val createdDate: LocalDate,

  @ManyToMany
  @JoinTable(
    name = "cas3_bedspace_characteristic_assignments",
    joinColumns = [JoinColumn(name = "bedspace_id")],
    inverseJoinColumns = [JoinColumn(name = "bedspace_characteristics_id")],
  )
  var characteristics: MutableList<Cas3BedspaceCharacteristicEntity>,
)

@Repository
interface Cas3BedspacesRepository : JpaRepository<Cas3BedspacesEntity, UUID> {

  @Query(
    """
      SELECT b.*
      FROM cas3_bedspaces b
      INNER JOIN (
        SELECT bk.bed_id, MIN(bk.arrival_date) earliest_arrival
        FROM cas3_bookings bk
        INNER JOIN cas3_bedspaces b ON bk.bed_id = b.id
        GROUP BY bk.bed_id
      ) earliest_bookings ON b.id = earliest_bookings.bed_id
      WHERE b.start_date > earliest_bookings.earliest_arrival
    """,
    nativeQuery = true,
  )
  fun findCas3BedspacesWithStartDateAfterBookingArrivalDate(): List<Cas3BedspacesEntity>

  @Query(
    """select bedspace from Cas3BedspacesEntity bedspace 
          where bedspace.id = :bedspaceId and bedspace.premises.id = :premisesId""",
  )
  fun findCas3Bedspace(premisesId: UUID, bedspaceId: UUID): Cas3BedspacesEntity?

  @Query(
    """
    SELECT b 
    FROM Cas3BedspacesEntity b 
    WHERE b.id = :bedspaceId AND 
    b.endDate IS NOT NULL AND b.endDate < :endDate
  """,
  )
  fun findArchivedBedspaceByBedspaceIdAndDate(bedspaceId: UUID, endDate: LocalDate): Cas3BedspacesEntity?

  fun findByPremisesId(premisesId: UUID): List<Cas3BedspacesEntity>
}
