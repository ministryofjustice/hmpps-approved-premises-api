package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listener.UserRevisionListener
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Explicitly defines the sequence generator used for Envers revision IDs.
 *
 * By default, Hibernate derives the sequence name from the entity name using
 * its default naming strategy. For the {@code RevInfo} entity, Hibernate expects a
 * sequence named {@code rev_info_seq}.
 *
 * Since the application uses a Flyway-managed schema instead of Hibernate
 * schema generation, the sequence mapping is defined explicitly to ensure
 * the entity configuration matches the database migration exactly.
 *
 * Alternatively, the sequence can be created in Flyway
 * {@code rev_info_seq} and Hibernate’s default sequence can be used
 * without explicitly defining a {@code @SequenceGenerator}.
 *
 * This approach reduces annotation boilerplate but introduces a dependency
 * on Hibernate’s implicit naming behavior.
 */

@Entity
@Table(name = "revinfo")
@RevisionEntity(UserRevisionListener::class)
class RevInfo(

  @Id
  @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "revinfo_seq_generator",
  )
  @SequenceGenerator(
    name = "revinfo_seq_generator",
    sequenceName = "revinfo_seq",
    allocationSize = 50,
  )
  @RevisionNumber
  var id: Int? = null,

  @RevisionTimestamp
  val timestamp: Long? = null,
  var username: String? = null,

) {
  /*
   *Added a few convenience functions to make it easier to work with the revision info as hibernate only
   * support longs for any field marked as revision timestamp
   */
  fun getRevisionInstant(): Instant = Instant.ofEpochMilli(timestamp ?: 0)

  fun getRevisionTime(): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(timestamp ?: 0),
    ZoneId.systemDefault(),
  )
}
