package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity

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
interface CaseRepository : JpaRepository<CaseEntity, UUID> {
  fun findByCrn(crn: String): CaseEntity?
}

@Entity
@Table(name = "cases")
data class CaseEntity(

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
