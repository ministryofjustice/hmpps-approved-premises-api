package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import java.util.*


@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_premises")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
class Cas3PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  var postcode: String,
  var addressLine1: String,
  var addressLine2: String?,
  var town: String?,

  @ManyToOne
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,

  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,

  @Enumerated(value = EnumType.STRING)
  var status: PropertyStatus,
  var turnaroundWorkingDayCount: Int,
  var notes: String,
)

@Repository
interface Cas3PremisesRepository : JpaRepository<Cas3PremisesEntity, UUID>
