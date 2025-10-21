package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import java.util.UUID

@Repository
interface CacheRefreshExclusionsInmateDetailsRepository : JpaRepository<Cas2UserEntity, UUID> {
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
