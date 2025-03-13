package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import java.util.UUID

@Entity
@Table(name = "cas1_change_request_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas1ChangeRequestReasonEntity(
  @Id
  val id: UUID,
  val code: String,
  @Enumerated(EnumType.STRING)
  val changeRequestType: Cas1ChangeRequestType,
  val archived: Boolean,
)
