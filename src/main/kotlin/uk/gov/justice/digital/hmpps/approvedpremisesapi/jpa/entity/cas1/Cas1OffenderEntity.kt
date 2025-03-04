package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "cas1_offenders")
data class Cas1OffenderEntity(
  @Id
  val id: UUID,
  val crn: String,
  val nomsNumber: String?,
  /**
   * The offender name. This should only be used for search purposes (i.e. SQL)
   * If returning the offender name to the user, use the [OffenderService], which
   * will consider any LAO restrictions
   */
  val name: String,
  val tier: String?,
  val createdAt: OffsetDateTime,
  val lastUpdatedAt: OffsetDateTime,

)
