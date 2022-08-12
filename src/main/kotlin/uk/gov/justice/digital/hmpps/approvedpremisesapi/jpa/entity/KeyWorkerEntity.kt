package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface KeyWorkerRepository : JpaRepository<KeyWorkerEntity, UUID>

@Entity
@Table(name = "key_workers")
data class KeyWorkerEntity(
  @Id
  var id: UUID,
  var name: String,
  var isActive: Boolean,
  @OneToMany(mappedBy = "keyWorker")
  var bookings: MutableList<BookingEntity>
)
