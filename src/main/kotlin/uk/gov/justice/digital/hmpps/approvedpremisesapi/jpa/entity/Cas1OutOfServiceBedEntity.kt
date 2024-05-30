package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Repository
interface Cas1OutOfServiceBedRepository : JpaRepository<Cas1OutOfServiceBedEntity, UUID>

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
  val reason: Cas1OutOfServiceBedReasonEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  val bed: BedEntity,
  val createdAt: OffsetDateTime,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val referenceNumber: String?,
  val notes: String?,
)
