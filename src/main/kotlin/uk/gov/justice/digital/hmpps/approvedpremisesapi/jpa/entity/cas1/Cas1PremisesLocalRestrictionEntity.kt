package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "cas1_premises_local_restrictions")
data class Cas1PremisesLocalRestrictionEntity(
  @Id
  val id: UUID,
  val description: String,
  val createdAt: OffsetDateTime,
  val createdByUserId: UUID,
  val approvedPremisesId: UUID,
  val archived: Boolean,
)
