package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface PostcodeDistrictRepository : JpaRepository<PostCodeDistrictEntity, UUID> {

  fun findByOutcode(outcode: String): PostCodeDistrictEntity?
}

@Entity
@Table(name = "postcode_districts")
data class PostCodeDistrictEntity(
  @Id
  val id: UUID,
  val outcode: String,
  val latitude: Double,
  val longitude: Double,
  val point: Point,
)
