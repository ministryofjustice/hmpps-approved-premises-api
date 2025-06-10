package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_premises")
@Inheritance(strategy = InheritanceType.JOINED)
class Cas3PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  var postcode: String,
  var addressLine1: String,
  var addressLine2: String?,
  var town: String?,

  @Enumerated(value = EnumType.STRING)
  var status: PropertyStatus,
  var notes: String,

  @ManyToOne
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity,

  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,

  @OneToMany(mappedBy = "premises")
  var bedspaces: MutableList<Cas3BedspacesEntity>,

  @ManyToMany
  @JoinTable(
    name = "cas3_premises_characteristic_assignments",
    joinColumns = [JoinColumn(name = "premises_id")],
    inverseJoinColumns = [JoinColumn(name = "premises_characteristics_id")],
  )
  var characteristics: MutableList<Cas3PremisesCharacteristicEntity>,

)

@Repository
interface Cas3PremisesRepository : JpaRepository<Cas3PremisesEntity, UUID>
