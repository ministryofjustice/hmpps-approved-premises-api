package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface CacheRefreshExclusionsInmateDetailsRepository : JpaRepository<CacheRefreshExclusionsInmateDetailsEntity, UUID> {
  @Query("SELECT DISTINCT(nomsNumber) FROM CacheRefreshExclusionsInmateDetailsEntity")
  fun getDistinctNomsNumbers(): List<String>
}

@Entity
@Table(name = "cache_refresh_exclusions_inmate_details")
data class CacheRefreshExclusionsInmateDetailsEntity(
  @Id
  val nomsNumber: String,
  val description: String,
)
