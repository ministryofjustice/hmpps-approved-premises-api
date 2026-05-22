package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listener.UserRevisionListener
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@RevisionEntity(UserRevisionListener::class)
class RevInfo(
  @Id
  @GeneratedValue
  @RevisionNumber
  var id: Int? = null,

  @RevisionTimestamp
  val timestamp: Long? = null,
  var username: String? = null,

) {
  fun getRevisionInstant(): Instant = Instant.ofEpochMilli(timestamp ?: 0)

  fun getRevisionTime(): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(timestamp ?: 0),
    ZoneId.systemDefault(),
  )
}
