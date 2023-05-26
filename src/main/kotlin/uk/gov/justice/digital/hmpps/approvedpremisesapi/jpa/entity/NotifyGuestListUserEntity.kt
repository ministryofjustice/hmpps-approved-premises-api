package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface NotifyGuestListUserRepository : JpaRepository<NotifyGuestListUserEntity, UUID>

@Entity
@Table(name = "notify_guest_list_users")
data class NotifyGuestListUserEntity(
  @Id
  val userId: UUID
)
