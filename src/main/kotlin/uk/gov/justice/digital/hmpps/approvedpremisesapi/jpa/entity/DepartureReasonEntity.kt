package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "departure_reasons")
data class DepartureReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean
) {
  override fun toString() = "DepartureReasonEntity:$id"
}
