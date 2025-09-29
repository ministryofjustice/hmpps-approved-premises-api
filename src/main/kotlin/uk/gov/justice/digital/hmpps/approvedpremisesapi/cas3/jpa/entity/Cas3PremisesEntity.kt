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
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
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
  var characteristics: MutableList<Cas3PremisesCharacteristicEntity>,

  var createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime? = null,

) {
  fun isPremisesScheduledToArchive(): Boolean = status == Cas3PremisesStatus.archived && endDate != null && endDate!! > LocalDate.now()
  fun isPremisesArchived(): Boolean = (endDate != null && endDate!! <= LocalDate.now()) || startDate.isAfter(LocalDate.now())
}

@Repository
interface Cas3PremisesRepository : JpaRepository<Cas3PremisesEntity, UUID>
