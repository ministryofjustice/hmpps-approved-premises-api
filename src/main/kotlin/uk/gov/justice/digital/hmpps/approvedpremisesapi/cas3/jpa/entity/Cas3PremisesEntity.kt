package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceArchived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceOnline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper.isCas3BedspaceUpcoming
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_premises")
@Inheritance(strategy = InheritanceType.JOINED)
data class Cas3PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  var postcode: String,
  var addressLine1: String,
  var addressLine2: String?,
  var town: String?,

  @Enumerated(value = EnumType.STRING)
  var status: Cas3PremisesStatus,
  var notes: String,
  @Column(name = "start_date")
  var startDate: LocalDate,
  @Column(name = "end_date")
  var endDate: LocalDate?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,

  @Column(name = "turnaround_working_days")
  var turnaroundWorkingDays: Int,

  @OneToMany(mappedBy = "premises", fetch = FetchType.LAZY)
  var bedspaces: MutableList<Cas3BedspacesEntity>,

  @OneToMany(mappedBy = "premises", fetch = FetchType.LAZY)
  var bookings: MutableList<Cas3BookingEntity>,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "cas3_premises_characteristic_assignments",
    joinColumns = [JoinColumn(name = "premises_id")],
    inverseJoinColumns = [JoinColumn(name = "premises_characteristics_id")],
  )
  var characteristics: MutableList<Cas3PremisesCharacteristicEntity> = mutableListOf(),

  var createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime? = null,

) {

  fun isPremisesScheduledToArchive(): Boolean = status == Cas3PremisesStatus.archived && endDate != null && endDate!! > LocalDate.now()
  fun isPremisesArchived(): Boolean = (endDate != null && endDate!! <= LocalDate.now()) || startDate.isAfter(LocalDate.now())

  fun countOnlineBedspaces() = this.bedspaces.count { isCas3BedspaceOnline(it.startDate, it.endDate) }
  fun countUpcomingBedspaces() = this.bedspaces.count { isCas3BedspaceUpcoming(it.startDate) }
  fun countArchivedBedspaces() = this.bedspaces.count { isCas3BedspaceArchived(it.endDate) }
}

@Repository
interface Cas3PremisesRepository : JpaRepository<Cas3PremisesEntity, UUID> {
  fun existsByNameIgnoreCaseAndProbationDeliveryUnitId(
    name: String,
    probationDeliveryUnitId: UUID,
  ): Boolean

  @Query(
    """
SELECT
          p.id as id,
          p.name as name,
          p.address_line1 as addressLine1,
          p.address_line2 as addressLine2,
          p.postcode as postcode,
          p.town as town,
          pdu.name as pdu,
          la.name as localAuthorityAreaName,
          bs.id as bedspaceId,
          bs.reference as bedspaceReference,
          CASE 
            WHEN bs.end_date <= CURRENT_DATE THEN 'archived' 
            WHEN bs.start_date IS NOT NULL AND bs.start_date > CURRENT_DATE THEN 'upcoming' 
            WHEN bs.id IS NOT NULL THEN 'online'
            ELSE NULL END as bedspaceStatus
      FROM
          cas3_premises p
          INNER JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
          INNER JOIN probation_regions pr ON pdu.probation_region_id = pr.id
          LEFT JOIN local_authority_areas la ON p.local_authority_area_id = la.id
          LEFT JOIN cas3_bedspaces bs on bs.premises_id = p.id
      WHERE pr.id = :regionId
        AND (:postcodeOrAddress is null
          OR lower(p.town) LIKE CONCAT('%',lower(:postcodeOrAddress),'%')
          OR lower(p.postcode) LIKE CONCAT('%',lower(:postcodeOrAddress),'%')
          OR lower(p.address_line1) LIKE CONCAT('%',lower(:postcodeOrAddress),'%')
          OR lower(p.address_line2) LIKE CONCAT('%',lower(:postcodeOrAddress),'%')
          OR lower(replace(p.postcode, ' ', '')) LIKE CONCAT('%',lower(:postcodeOrAddressWithoutWhitespace),'%')
          )
     AND (
         (:premisesStatus = 'online' AND (p.end_date IS NULL OR p.end_date > CURRENT_DATE) AND p.start_date <= CURRENT_DATE)
         OR (:premisesStatus = 'archived' AND ((p.end_date IS NOT NULL AND p.end_date <= CURRENT_DATE) OR p.start_date > CURRENT_DATE))
         OR :premisesStatus IS NULL)
      """,
    nativeQuery = true,
  )
  fun findAllCas3PremisesSummary(regionId: UUID, postcodeOrAddress: String?, postcodeOrAddressWithoutWhitespace: String?, premisesStatus: String?): List<Cas3PremisesSummaryResult>
}

interface Cas3PremisesSummaryResult {
  val id: UUID
  val name: String
  val addressLine1: String
  val addressLine2: String?
  val postcode: String
  val town: String?
  val pdu: String
  val localAuthorityAreaName: String?
  val bedspaceId: UUID?
  val bedspaceReference: String?
  val bedspaceStatus: Cas3BedspaceStatus?
}
