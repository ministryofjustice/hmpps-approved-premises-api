package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface PersonRepository : JpaRepository<PersonEntity, UUID>

@Entity
@Table(name = "people")
data class PersonEntity(
  @Id
  val id: UUID,
  val crn: String,
  val name: String,
  @OneToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PersonEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (name != other.name) return false

    return true
  }

  override fun hashCode() = Objects.hash(id, crn, name)

  override fun toString() = "PersonEntity:$id"
}
