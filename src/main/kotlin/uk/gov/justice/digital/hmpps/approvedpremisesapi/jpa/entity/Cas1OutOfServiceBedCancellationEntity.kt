package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface Cas1OutOfServiceBedCancellationRepository : JpaRepository<Cas1OutOfServiceBedCancellationEntity, UUID>

@Entity
@Table(name = "cas1_out_of_service_bed_cancellations")
class Cas1OutOfServiceBedCancellationEntity(
  @Id
  val id: UUID,
  val createdAt: OffsetDateTime,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "out_of_service_bed_id")
  val outOfServiceBed: Cas1OutOfServiceBedEntity,
)
