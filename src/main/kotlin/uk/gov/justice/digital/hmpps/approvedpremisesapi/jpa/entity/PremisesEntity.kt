package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID> {
  @Query("SELECT p FROM PremisesEntity p WHERE TYPE(p) = :type")
  fun <T : PremisesEntity> findAllByType(type: Class<T>): List<PremisesEntity>
}

@Entity
@Table(name = "premises")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class PremisesEntity(
  @Id
  val id: UUID,
  val name: String,
  val addressLine1: String,
  var postcode: String,
  var totalBeds: Int,
  val deliusTeamCode: String,
  val notes: String,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  val probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  val localAuthorityArea: LocalAuthorityAreaEntity,
  @OneToMany(mappedBy = "premises")
  val bookings: MutableList<BookingEntity>,
  @OneToMany(mappedBy = "premises")
  var lostBeds: MutableList<LostBedsEntity>
)

@Entity
@DiscriminatorValue("CAS1")
@Table(name = "approved_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class ApprovedPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  postcode: String,
  totalBeds: Int,
  deliusTeamCode: String,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  val apCode: String,
  val qCode: String
) : PremisesEntity(id, name, addressLine1, postcode, totalBeds, deliusTeamCode, notes, probationRegion, localAuthorityArea, bookings, lostBeds)

@Entity
@DiscriminatorValue("CAS3")
@Table(name = "temporary_accommodation_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class TemporaryAccommodationPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  postcode: String,
  totalBeds: Int,
  deliusTeamCode: String,
  notes: String,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>
) : PremisesEntity(
  id, name, addressLine1, postcode, totalBeds, deliusTeamCode, notes, probationRegion, localAuthorityArea, bookings, lostBeds
)
