package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ApAreaRepository : JpaRepository<ApAreaEntity, UUID> {
  fun findByName(name: String): ApAreaEntity?
  fun findByIdentifier(name: String): ApAreaEntity?

  @Modifying
  @Query("UPDATE ApAreaEntity set emailAddress = :emailAddress where id = :id")
  fun updateEmailAddress(id: UUID, emailAddress: String)
}

@Entity
@Table(name = "ap_areas")
data class ApAreaEntity(
  @Id
  val id: UUID,
  val name: String,
  val identifier: String,
  @OneToMany(mappedBy = "apArea")
  val probationRegions: MutableList<ProbationRegionEntity>,
  val emailAddress: String?,
)
