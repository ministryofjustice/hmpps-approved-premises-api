package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1OffenderRepository : JpaRepository<Cas1OffenderEntity, UUID> {
  fun findByCrn(crn: String): Cas1OffenderEntity?
}

@Entity
@Table(name = "cas1_offenders")
data class Cas1OffenderEntity(

  @Id
  val id: UUID,
  var crn: String,
  var nomsNumber: String?,
  /**
   * The offender name. This should only be used for search purposes (i.e. SQL)
   * If returning the offender name to the user, use the [OffenderService], which
   * will consider any LAO restrictions
   */
  var name: String,
  var tier: String?,
  val createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime,
  @Version
  var version: Long = 1,
) {

  @PreUpdate
  fun preUpdate() {
    lastUpdatedAt = OffsetDateTime.now()
  }
}
