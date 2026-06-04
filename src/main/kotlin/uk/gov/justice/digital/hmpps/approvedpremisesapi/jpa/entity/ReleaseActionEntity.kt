package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.util.UUID

@Entity
@Table(name = "release_action")
@Audited
class ReleaseActionEntity(
  @Id
  val id: UUID,

  var description: String,
  var actionCadence: String,
  var otherInformation: String? = null,
)
