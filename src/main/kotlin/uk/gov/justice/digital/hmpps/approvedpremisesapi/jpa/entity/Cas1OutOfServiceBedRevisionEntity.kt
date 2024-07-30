package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.EnumSet
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevisionType as ApiRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType as DomainRevisionType

@Repository
interface Cas1OutOfServiceBedRevisionRepository : JpaRepository<Cas1OutOfServiceBedRevisionEntity, UUID>

@Entity
@Table(name = "cas1_out_of_service_bed_revisions")
data class Cas1OutOfServiceBedRevisionEntity(
  @Id
  val id: UUID,
  val createdAt: OffsetDateTime,
  @Enumerated(EnumType.STRING)
  val revisionType: DomainRevisionType,
  var startDate: LocalDate,
  var endDate: LocalDate,
  var referenceNumber: String?,
  var notes: String?,
  @ManyToOne
  @JoinColumn(name = "out_of_service_bed_reason_id")
  var reason: Cas1OutOfServiceBedReasonEntity,
  @ManyToOne
  @JoinColumn(name = "out_of_service_bed_id")
  val outOfServiceBed: Cas1OutOfServiceBedEntity,
  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity?,
  @Column(name = "change_type")
  val changeTypePacked: Long,
)

enum class Cas1OutOfServiceBedRevisionType {
  INITIAL,
  UPDATE,
}

@Suppress("detekt:MagicNumber")
enum class Cas1OutOfServiceBedRevisionChangeType(private val value: Long, val apiValue: ApiRevisionType) {
  START_DATE(0b0000_0001, ApiRevisionType.updatedStartDate),
  END_DATE(0b0000_0010, ApiRevisionType.updatedEndDate),
  REFERENCE_NUMBER(0b0000_0100, ApiRevisionType.updatedReferenceNumber),
  REASON(0b0000_1000, ApiRevisionType.updatedReason),
  NOTES(0b0001_0000, ApiRevisionType.updatedNotes),
  ;

  companion object {
    const val NO_CHANGE: Long = 0

    fun pack(values: EnumSet<Cas1OutOfServiceBedRevisionChangeType>): Long = values
      .fold(0L) { acc, changeType -> acc or changeType.value }

    fun unpack(value: Long): EnumSet<Cas1OutOfServiceBedRevisionChangeType> {
      val result = Cas1OutOfServiceBedRevisionChangeType.entries.filter { (it.value and value) != 0L }

      return EnumSet
        .noneOf(Cas1OutOfServiceBedRevisionChangeType::class.java)
        .apply { addAll(result) }
    }
  }
}
