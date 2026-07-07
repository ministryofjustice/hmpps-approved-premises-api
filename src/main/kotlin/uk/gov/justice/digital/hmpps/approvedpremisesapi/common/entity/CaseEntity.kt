package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.BackfillCaseSummaryMigrationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.asHibernateProxy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.getHibernateClass
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface CaseRepository : JpaRepository<CaseEntity, UUID> {
  fun findByCrn(crn: String): CaseEntity?

  @Query(
    """
    SELECT DISTINCT ON (crn)
        crn,
        name,
        noms_number AS nomsNumber
    FROM (
        SELECT
            crn,
            name,
            CAST(NULL AS text) AS noms_number
        FROM offline_applications
    
        UNION ALL
    
        SELECT
            crn,
            CAST(NULL AS text) AS name,
            noms_number
        FROM cas_2_applications
    
        UNION ALL
    
        SELECT crn, name, noms_number
        FROM applications a
        INNER JOIN temporary_accommodation_applications taa
            ON taa.id = a.id
    ) missing_cases
    WHERE NOT EXISTS (
        SELECT 1
        FROM cases c
        WHERE c.crn = missing_cases.crn
    )
    """,
    nativeQuery = true,
  )
  fun findAllUniqueCrnsMissingFromCases(): List<BackfillCaseSummaryMigrationDto>
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
  val createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime,
  @Column(name = "tier_v2", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  var tierV2: Tier?,
  @Column(name = "tier_v3", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  var tierV3: Tier?,
  @Version
  var version: Long = 1,
) {

  @PreUpdate
  fun preUpdate() {
    lastUpdatedAt = OffsetDateTime.now()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (this.getHibernateClass() != other.getHibernateClass()) return false
    other as CaseEntity

    return id == other.id
  }

  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "CaseEntity(id=$id,crn=$crn)"
}
