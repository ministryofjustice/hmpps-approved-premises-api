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

  fun findByCrnIn(crns: List<String>): List<CaseEntity>

  @Query(
    """
        WITH all_applications_crn AS (
              SELECT
                crn,
                name,
                NULL::text AS noms_number,
                created_at
              FROM offline_applications
            
              UNION ALL
            
              SELECT
                crn,
                NULL::text AS name,
                noms_number,
                created_at
              FROM cas_2_applications
            
              UNION ALL
            
            SELECT
                a.crn,
                COALESCE(ta.name, ap.name) AS name,
                a.noms_number,
                a.created_at
            FROM applications a
            LEFT JOIN temporary_accommodation_applications ta
                ON ta.id = a.id
            LEFT JOIN approved_premises_applications ap
                ON ap.id = a.id
            ),
        latest_applications_crn AS (
              SELECT
                crn,
                name,
                noms_number,
                created_at
              FROM (
                SELECT
                  *,
                  ROW_NUMBER() OVER (
                    PARTITION BY crn
                    ORDER BY created_at DESC
                  ) AS row_num
                FROM all_applications_crn
              ) ranked
              WHERE row_num = 1
            )
            SELECT
              la.crn,
              la.name,
              la.noms_number AS nomsNumber,
              (c.id IS NOT NULL) AS case_exists,
              (c.tier_v2 IS NOT NULL) AS has_tier_v2,
              (c.tier_v3 IS NOT NULL) AS has_tier_v3
            FROM latest_applications_crn la
            LEFT JOIN cases c
              ON c.crn = la.crn
    """,
    nativeQuery = true,
  )
  fun findUniqueCrnsForBackfill(): List<BackfillCaseSummaryMigrationDto>
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
  var name: String?,
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
