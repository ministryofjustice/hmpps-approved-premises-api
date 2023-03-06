package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.math.BigDecimal
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "postcode_districts")
data class PostCodeDistrictEntity(
  @Id
  val id: UUID,
  val outcode: String,
  val latitude: BigDecimal,
  val longitude: BigDecimal
)
