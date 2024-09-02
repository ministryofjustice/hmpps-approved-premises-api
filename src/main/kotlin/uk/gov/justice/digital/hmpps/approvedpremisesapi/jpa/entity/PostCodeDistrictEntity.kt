package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PostcodeDistrictRepository : JpaRepository<PostCodeDistrictEntity, UUID> {

  fun findByOutcode(outcode: String): PostCodeDistrictEntity?
}

@Entity
@Table(name = "postcode_districts")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class PostCodeDistrictEntity(
  @Id
  val id: UUID,
  val outcode: String,
  val latitude: Double,
  val longitude: Double,
  val point: Point,
)
