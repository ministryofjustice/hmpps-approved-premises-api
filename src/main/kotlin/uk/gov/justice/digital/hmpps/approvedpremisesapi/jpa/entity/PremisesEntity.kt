package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID>

@Entity
@Table(name = "premises")
data class PremisesEntity(
  @Id
  val id: UUID,
  val name: String,
  val apCode: String,
  val address_line_1: String,
  var postcode: String,
  var totalBeds: Int,
  val deliusTeamCode: String,
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
